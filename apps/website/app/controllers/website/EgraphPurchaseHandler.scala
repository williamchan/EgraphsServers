package controllers.website

import models._
import enums._
import play.mvc.Scope.Flash
import services.mail.TransactionalMail
import services.{Utils, AppConfig}
import controllers.WebsiteControllers
import services.payment.{Charge, Payment}
import scala.Predef._
import sjson.json.Serializer
import services.db.{DBSession, TransactionSerializable}
import services.logging.Logging
import exception.InsufficientInventoryException
import play.mvc.results.Redirect
import services.http.ServerSessionFactory
import java.text.SimpleDateFormat
import org.apache.commons.mail.HtmlEmail
import org.joda.money.Money
import services.http.forms.purchase.CheckoutShippingForm
import WebsiteControllers.{reverse, getOrderConfirmation}
import models.Customer
import models.CashTransaction
import models.FailedPurchaseData

/**
 * Performs the meat of the purchase controller's interaction with domain
 * objects. Having it as a separate case class makes it more testable.
 */
// TODO(erem): Refactor this class to be injected
case class EgraphPurchaseHandler(
  recipientName: String,
  recipientEmail: String,
  buyerName: String,
  buyerEmail: String,
  stripeTokenId: String,
  desiredText: Option[String],
  personalNote: Option[String],
  celebrity: Celebrity,
  product: Product,
  totalAmountPaid: Money,
  billingPostalCode: String,
  printingOption: PrintingOption = PrintingOption.DoNotPrint,
  shippingForm: Option[CheckoutShippingForm.Valid] = None,
  writtenMessageRequest: WrittenMessageRequest = WrittenMessageRequest.SpecificMessage,
  flash: Flash = Flash.current(),
  mail: TransactionalMail = AppConfig.instance[TransactionalMail],
  customerStore: CustomerStore = AppConfig.instance[CustomerStore],
  accountStore: AccountStore = AppConfig.instance[AccountStore],
  dbSession: DBSession = AppConfig.instance[DBSession],
  payment: Payment = AppConfig.instance[Payment],
  serverSessions: ServerSessionFactory = AppConfig.instance[ServerSessionFactory],
  isDemo: Boolean = false)
  extends Logging
{

  private val dateFormat = new SimpleDateFormat("MMMM dd, yyyy")

  private def purchaseData: String = Serializer.SJSON.toJSON(Map(
    "recipientName" -> recipientName,
    "recipientEmail" -> recipientEmail,
    "buyerName" -> buyerName,
    "buyerEmail" -> buyerEmail,
    "stripeTokenId" -> stripeTokenId,
    "desiredText" -> desiredText.getOrElse(""),
    "personalNote" -> personalNote.getOrElse(""),
    "productId" -> product.id,
    "productPrice" -> totalAmountPaid.getAmount
  ))

  /**
   * Performs the purchase the purchase with error handling.
   * @return A Redirect to either an order confirmation page or some error page.
   */
  def execute(): Redirect = {
    val errorOrOrder = performPurchase

    val redirect = errorOrOrder.fold(
      (error) => error match {
        case stripeError: PurchaseFailedStripeError =>
          //Attempt Stripe charge. If a credit card-related error occurred, redirect to purchase screen.
          play.Logger.error(stripeError.stripeException.getStackTraceString)
          return new Redirect(reverse(WebsiteControllers.getStorefrontCreditCardError(celebrity.urlSlug, product.urlSlug, stripeError.stripeException.getLocalizedMessage)).url)
        case _: PurchaseFailedInsufficientInventory =>
          //A redirect to the insufficient inventory page
          new Redirect(reverse(WebsiteControllers.getStorefrontNoInventory(celebrity.urlSlug, product.urlSlug)).url)
        case _: PurchaseFailedError =>
          return new Redirect(reverse(WebsiteControllers.getStorefrontPurchaseError(celebrity.urlSlug, product.urlSlug)).url)
      },
      (successfulOrder) =>
        //A redirect to the order confirmation page
        new Redirect(reverse(getOrderConfirmation(successfulOrder.id)).url)
    )

    redirect
  }

  def performPurchase(): Either[PurchaseFailed, Order] = {
    val charge = try {
      payment.charge(totalAmountPaid, stripeTokenId, "Egraph Order from " + buyerEmail)
    } catch {
      case stripeException: com.stripe.exception.StripeException => {
        val failedPurchaseData = saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = "Credit card issue: " + stripeException.getLocalizedMessage)
        return Left(PurchaseFailedStripeError(failedPurchaseData, stripeException))
      }
    }

    // Persist the Order. This is executed in its own database transaction.
    val (order: Order, _: Customer, _: Customer, cashTransaction: CashTransaction, maybePrintOrder: Option[PrintOrder]) = try {
      persistOrder(buyerEmail = buyerEmail,
        buyerName = buyerName,
        recipientEmail = recipientEmail,
        recipientName = recipientName,
        personalNote = personalNote,
        desiredText = desiredText,
        stripeTokenId = stripeTokenId,
        totalAmountPaid = totalAmountPaid,
        billingPostalCode = billingPostalCode,
        printingOption = printingOption,
        shippingForm = shippingForm,
        writtenMessageRequest = writtenMessageRequest,
        isDemo = isDemo,
        charge = charge,
        celebrity = celebrity,
        product = product,
        dbSession = dbSession,
        accountStore = accountStore,
        customerStore = customerStore)
    } catch {
      case e: InsufficientInventoryException => {
        payment.refund(charge.id)
        val failedPurchaseData = saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = e.getLocalizedMessage)
        return Left(PurchaseFailedInsufficientInventory(failedPurchaseData))
      }
      case e: Exception => {
        payment.refund(charge.id)
        val failedPurchaseData = saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = e.getLocalizedMessage)
        return Left(PurchaseFailedError(failedPurchaseData))
      }
    }

    // If the Stripe charge and Order persistence executed successfully, send a confirmation email and redirect to a confirmation page
    sendOrderConfirmationEmail(buyerName = buyerName, buyerEmail = buyerEmail, recipientName = recipientName, recipientEmail = recipientEmail, celebrity, product, order, cashTransaction, maybePrintOrder, mail)
    flash.put("orderId", order.id)

    // Clear out the shopping cart and redirect
    serverSessions.celebrityStorefrontCart(celebrity.id).emptied.save()
    Right(order)
  }

  // Failure base type
  sealed abstract class PurchaseFailed(val failedPurchaseData: FailedPurchaseData)

  // Our two failure cases for if the payment vendor failed or there was insufficient inventory
  case class PurchaseFailedStripeError(override val failedPurchaseData: FailedPurchaseData, stripeException: com.stripe.exception.StripeException) extends PurchaseFailed(failedPurchaseData)
  case class PurchaseFailedInsufficientInventory(override val failedPurchaseData: FailedPurchaseData) extends PurchaseFailed(failedPurchaseData)
  case class PurchaseFailedError(override val failedPurchaseData: FailedPurchaseData) extends PurchaseFailed(failedPurchaseData)

  private def persistOrder(dbSession: DBSession,
                           customerStore: CustomerStore,
                           buyerEmail: String,
                           buyerName: String,
                           recipientEmail: String,
                           product: Product,
                           recipientName: String,
                           personalNote: Option[String],
                           desiredText: Option[String],
                           stripeTokenId: String,
                           charge: Charge,
                           totalAmountPaid: Money,
                           billingPostalCode: String,
                           printingOption: PrintingOption,
                           shippingForm: Option[CheckoutShippingForm.Valid],
                           writtenMessageRequest: WrittenMessageRequest,
                           isDemo: Boolean,
                           accountStore: AccountStore,
                           celebrity: Celebrity): (Order, Customer, Customer, CashTransaction, Option[PrintOrder]) = {
    dbSession.connected(TransactionSerializable) {
      // Get buyer and recipient accounts and create customer face if necessary
      val buyer = customerStore.findOrCreateByEmail(buyerEmail, buyerName)
      val recipient = if (buyerEmail == recipientEmail) {
        buyer
      } else {
        customerStore.findOrCreateByEmail(recipientEmail, recipientName)
      }

      // Persist the Order with the Stripe charge info.
      val shippingAddress = shippingForm match {
        case Some(form) => {
          val addressLine2Part = form.addressLine2 match {
            case Some(s) => s + ", "
            case None => ""
          }
          Some(form.name + ", " +
            form.addressLine1 + ", " +
            addressLine2Part +
            form.city + ", " +
            form.state + " " +
            form.postalCode)
        }
        case _ => None
      }
      var order = buyer.buy(product, recipient, recipientName = recipientName, messageToCelebrity = personalNote, requestedMessage = desiredText)
        .copy(amountPaidInCurrency = product.priceInCurrency)
        .withWrittenMessageRequest(writtenMessageRequest)
        .withPaymentStatus(PaymentStatus.Charged)
        .save()
      var cashTransaction = CashTransaction(accountId = buyer.account.id,
        orderId = Some(order.id),
        stripeCardTokenId = Some(stripeTokenId),
        stripeChargeId = Some(charge.id),
        billingPostalCode = Some(billingPostalCode)
      ).withCash(totalAmountPaid).withCashTransactionType(CashTransactionType.EgraphPurchase).save()

      val maybePrintOrder = if (printingOption == PrintingOption.HighQualityPrint) {
        val printOrder = PrintOrder(orderId = order.id, amountPaidInCurrency = PrintOrder.pricePerPrint, shippingAddress = shippingAddress.getOrElse("")).save() // update CashTransaction
        cashTransaction = cashTransaction.copy(printOrderId = Some(printOrder.id)).save()
        Some(printOrder)
      } else {
        None
      }

      if (isDemo) {
        order = order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      }

      (order, buyer, recipient, cashTransaction, maybePrintOrder)
    }
  }

  private def saveFailedPurchaseData(dbSession: DBSession, purchaseData: String, errorDescription: String): FailedPurchaseData =  {
    dbSession.connected(TransactionSerializable) {
      FailedPurchaseData(purchaseData = purchaseData, errorDescription = errorDescription.take(128 /*128 is the column width*/)).save()
    }
  }

  private def sendOrderConfirmationEmail(buyerName: String,
                                         buyerEmail: String,
                                         recipientName: String,
                                         recipientEmail: String,
                                         celebrity: Celebrity,
                                         product: Product,
                                         order: Order,
                                         cashTransaction: CashTransaction,
                                         maybePrintOrder: Option[PrintOrder],
                                         mail: TransactionalMail) {
    import services.Finance.TypeConversions._
    val email = new HtmlEmail()
    email.setFrom("noreply@egraphs.com", "Egraphs")
    email.addTo(buyerEmail, buyerName)
    email.setSubject("Order Confirmation")
    val faqHowLongLink = Utils.absoluteUrl(Utils.lookupUrl("WebsiteControllers.getFAQ")) + "#how-long"
    val html = views.frontend.html.email_order_confirmation(
      buyerName = buyerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      celebrityName = celebrity.publicName,
      productName = product.name,
      orderDate = dateFormat.format(order.created),
      orderId = order.id.toString,
      pricePaid = cashTransaction.cash.formatSimply,
      deliveredByDate = dateFormat.format(order.expectedDate.get), // all new Orders have expectedDate... will turn this into Date instead of Option[Date]
      faqHowLongLink = faqHowLongLink,
      hasPrintOrder = maybePrintOrder.isDefined
    )
    email.setHtmlMsg(html.toString())
    email.setTextMsg(views.frontend.html.email_order_confirmation_text(
      buyerName = buyerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      celebrityName = celebrity.publicName,
      productName = product.name,
      orderDate = dateFormat.format(order.created),
      orderId = order.id.toString,
      pricePaid = cashTransaction.cash.formatSimply,
      deliveredByDate = dateFormat.format(order.expectedDate.get),
      faqHowLongLink = faqHowLongLink,
      hasPrintOrder = maybePrintOrder.isDefined
    ).toString())
    mail.send(email)
  }
}

object EgraphPurchaseHandler extends Logging
