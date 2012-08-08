package controllers.website

import models._
import enums._
import play.mvc.Scope.Flash
import services.mail.Mail
import services.AppConfig
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
import scala.Some
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
  mail: Mail = AppConfig.instance[Mail],
  customerStore: CustomerStore = AppConfig.instance[CustomerStore],
  accountStore: AccountStore = AppConfig.instance[AccountStore],
  dbSession: DBSession = AppConfig.instance[DBSession],
  payment: Payment = AppConfig.instance[Payment],
  serverSessions: ServerSessionFactory = AppConfig.instance[ServerSessionFactory],
  isDemo: Boolean = false)
{

  private val dateFormat = new SimpleDateFormat("MMMM dd, yyyy")

  def execute(): Any = {
    val purchaseData: String = Serializer.SJSON.toJSON(Map(
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

    // Attempt Stripe charge. If a credit card-related error occurred, redirect to purchase screen.
    val charge = try {
      payment.charge(totalAmountPaid, stripeTokenId, "Egraph Order from " + buyerEmail)
    } catch {
      case stripeException: com.stripe.exception.StripeException => {
        saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = "Credit card issue: " + stripeException.getLocalizedMessage)
        return new Redirect(reverse(WebsiteControllers.getStorefrontCreditCardError(celebrity.urlSlug, product.urlSlug, stripeException.getLocalizedMessage)).url)
      }
    }

    // Persist the Order. This is executed in its own database transaction.
    val (order: Order, _: Customer, _: Customer, cashTransaction: CashTransaction) = try {
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
        isDemo = isDemo, charge = charge, celebrity = celebrity, product = product, dbSession = dbSession, accountStore = accountStore, customerStore = customerStore)
    } catch {
      case e: InsufficientInventoryException => {
        payment.refund(charge.id)
        saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = e.getLocalizedMessage)
        return new Redirect(reverse(WebsiteControllers.getStorefrontNoInventory(celebrity.urlSlug, product.urlSlug)).url)
      }
      case e: Exception => {
        payment.refund(charge.id)
        saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = e.getLocalizedMessage)
        return new Redirect(reverse(WebsiteControllers.getStorefrontPurchaseError(celebrity.urlSlug, product.urlSlug)).url)
      }
    }

    // If the Stripe charge and Order persistence executed successfully, send a confirmation email and redirect to a confirmation page
    sendOrderConfirmationEmail(buyerName = buyerName, buyerEmail = buyerEmail, recipientName = recipientName, recipientEmail = recipientEmail, celebrity, product, order, cashTransaction, mail)
    flash.put("orderId", order.id)

    // Clear out the shopping cart and redirect
    serverSessions.celebrityStorefrontCart(celebrity.id).emptied.save()

    new Redirect(reverse(getOrderConfirmation(order.id)).url)
  }

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
                           celebrity: Celebrity): (Order, Customer, Customer, CashTransaction) = {
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

      if (printingOption == PrintingOption.HighQualityPrint) {
        val printOrder = PrintOrder(orderId = order.id, amountPaidInCurrency = PrintOrder.pricePerPrint, shippingAddress = shippingAddress.getOrElse("")).save() // update CashTransaction
        cashTransaction = cashTransaction.copy(printOrderId = Some(printOrder.id)).save()
      }

      if (isDemo) {
        order = order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      }

      (order, buyer, recipient, cashTransaction)
    }
  }

  private def saveFailedPurchaseData(dbSession: DBSession, purchaseData: String, errorDescription: String) {
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
                                         mail: Mail) {
    import services.Finance.TypeConversions._
    val email = new HtmlEmail()
    email.setFrom("noreply@egraphs.com", "Egraphs")
    email.addTo(buyerEmail, buyerName)
    email.setSubject("Order Confirmation")
//    val emailLogoSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-logo.jpg")))
//    val emailFacebookSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-facebook.jpg")))
//    val emailTwitterSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-twitter.jpg")))
    val emailLogoSrc = ""
    val emailFacebookSrc = ""
    val emailTwitterSrc = ""
    val html = views.frontend.html.email_order_confirmation(
      buyerName = buyerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      celebrityName = celebrity.publicName,
      productName = product.name,
      orderDate = dateFormat.format(order.created),
      orderId = order.id.toString,
      pricePaid = cashTransaction.cash.formatSimply,
      deliveredyDate = dateFormat.format(order.expectedDate.get), // all new Orders have expectedDate... will turn this into Date instead of Option[Date]
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
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
      deliveredyDate = dateFormat.format(order.expectedDate.get)
    ).toString())
    mail.send(email)
  }
}

object EgraphPurchaseHandler extends Logging
