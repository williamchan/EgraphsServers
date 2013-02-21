package controllers.website

import com.google.inject._
import play.api.mvc._
import play.api.libs.json._
import models._
import models.enums._
import services.email.OrderConfirmationEmail
import services.mail.TransactionalMail
import services.payment.{Charge, Payment}
import services.db.{DBSession, TransactionSerializable}
import services.logging.Logging
import exception.InsufficientInventoryException
import play.api.mvc.Results.Redirect
import services.http.ServerSessionFactory
import org.joda.money.Money
import services.http.forms.purchase.CheckoutShippingForm
import controllers.routes.WebsiteControllers.getFAQ
import services._
import models.frontend.email.OrderConfirmationEmailViewModel
import services.http.EgraphsSession.Conversions._
import services.Finance.TypeConversions._
import _root_.frontend.formatting.DateFormatting.Conversions._

case class EgraphPurchaseHandlerServices @Inject() (
  customerStore: CustomerStore,
  accountStore: AccountStore,
  cashTransactionStore: CashTransactionStore,
  dbSession: DBSession,
  payment: Payment,
  serverSessions: ServerSessionFactory,
  consumerApp: ConsumerApplication
)

/**
 * Performs the meat of the purchase controller's interaction with domain
 * objects. Having it as a separate case class makes it more testable.
 * 
 * @param totalAmountPaid the amount to charge the credit card. All discounts should already be figured into totalAmountPaid
 * @param coupon coupon applied, if any
 */
// TODO(erem): Refactor this class to be injected
case class EgraphPurchaseHandler(
  recipientName: String,
  recipientEmail: String,
  buyerName: String,
  buyerEmail: String,
  stripeTokenId: Option[String],
  desiredText: Option[String],
  personalNote: Option[String],
  celebrity: Celebrity,
  product: Product,
  totalAmountPaid: Money,
  billingPostalCode: String,
  coupon: Option[Coupon] = None,
  printingOption: PrintingOption = PrintingOption.DoNotPrint,
  shippingForm: Option[CheckoutShippingForm.Valid] = None,
  writtenMessageRequest: WrittenMessageRequest = WrittenMessageRequest.SpecificMessage,
  services: EgraphPurchaseHandlerServices = AppConfig.instance[EgraphPurchaseHandlerServices],
  isDemo: Boolean = false
)(
  implicit request: RequestHeader
)
{
  import EgraphPurchaseHandler._

  private def purchaseData: JsValue = {
    Json.obj(
      "recipientName" -> recipientName,
      "recipientEmail" -> recipientEmail,
      "buyerName" -> buyerName,
      "buyerEmail" -> buyerEmail,
      "stripeTokenId" -> stripeTokenId.getOrElse[String](""),
      "desiredText" -> desiredText.getOrElse[String](""),
      "personalNote" -> personalNote.getOrElse[String](""),
      "productId" -> product.id,
      "productPrice" -> JsNumber(totalAmountPaid.getAmount)
    )
  }
  
  /**
   * Performs the purchase the purchase with error handling.
   * @return A Redirect to either an order confirmation page or some error page.
   */
  def execute(): Result = {
    val errorOrOrder = performPurchase()

    errorOrOrder.fold(
      (error) => error match {
        case stripeError: PurchaseFailedStripeError =>
          //Attempt Stripe charge. If a credit card-related error occurred, redirect to purchase screen.
          Redirect(
            controllers.routes.WebsiteControllers.getStorefrontCreditCardError(
              celebrity.urlSlug, 
              product.urlSlug, 
              Some(stripeError.stripeException.getLocalizedMessage)
            )
          )
        case _: PurchaseFailedInsufficientInventory =>
          //A redirect to the insufficient inventory page
          Redirect(controllers.routes.WebsiteControllers.getStorefrontNoInventory(celebrity.urlSlug, product.urlSlug))
        case _: PurchaseFailedError =>
          Redirect(controllers.routes.WebsiteControllers.getStorefrontPurchaseError(celebrity.urlSlug, product.urlSlug))
      },
      (successful) => successful match {
        case (successfulOrder, didCreateBuyer) =>
          //A redirect to the order confirmation page
          val successResult = Redirect(controllers.routes.WebsiteControllers.getOrderConfirmation(successfulOrder.id))
            .flashing(request.flash + ("orderId" -> successfulOrder.id.toString))
          if(didCreateBuyer) {
            successResult.withSession(request.session.withUsernameChanged)
          } else {
            successResult
          }
      }
    )
  }

  def performPurchase(): Either[PurchaseFailed, (Order, Boolean /*Did create buyer*/)] = {
    val charge = try {
      if (totalAmountPaid.isZero) {
        None
      } else {
        // We want this to throw if stripeTokenId is None when the amount to charge is non-zero
        Some(services.payment.charge(totalAmountPaid, stripeTokenId.get, "Egraph Order from " + buyerEmail))
      }
    } catch {
      case stripeException: com.stripe.exception.StripeException => {
        val failedPurchaseData = saveFailedPurchaseData(
          purchaseData = purchaseData, 
          errorDescription = "Credit card issue: " + stripeException.getLocalizedMessage
        )
        return Left(PurchaseFailedStripeError(failedPurchaseData, stripeException))
      }
    }

    // Persist the Order. This is executed in its own database transaction.
    // cashTransaction can be None if the order was free (due to coupons).
    // maybePrintOrder can be Some if printingOption is "HighQualityPrint".
    val (order: Order, _: Customer, _: Customer, cashTransaction: Option[_], maybePrintOrder: Option[_], didCreateBuyer: Boolean) = try {
      persistOrder(buyerEmail = buyerEmail,
        buyerName = buyerName,
        recipientEmail = recipientEmail,
        recipientName = recipientName,
        personalNote = personalNote,
        desiredText = desiredText,
        stripeTokenId = stripeTokenId,
        totalAmountPaid = totalAmountPaid,
        billingPostalCode = billingPostalCode,
        coupon = coupon,
        printingOption = printingOption,
        shippingForm = shippingForm,
        writtenMessageRequest = writtenMessageRequest,
        isDemo = isDemo,
        charge = charge,
        celebrity = celebrity,
        product = product)
    } catch {
      case e: InsufficientInventoryException => {
        charge.map(c => services.payment.refund(c.id))
        val failedPurchaseData = saveFailedPurchaseData(
          purchaseData = purchaseData, 
          errorDescription = e.getLocalizedMessage
        )
        return Left(PurchaseFailedInsufficientInventory(failedPurchaseData))
      }
      case e: Exception => {
        charge.map(c => services.payment.refund(c.id))
        val failedPurchaseData = saveFailedPurchaseData(
          purchaseData = purchaseData, 
          errorDescription = e.getLocalizedMessage
        )
        return Left(PurchaseFailedError(failedPurchaseData))
      }
    }

    // If the Stripe charge and Order persistence executed successfully, send a confirmation email and redirect to a confirmation page
    OrderConfirmationEmail(
      OrderConfirmationEmailViewModel(
        buyerName = buyerName,
        buyerEmail = buyerEmail,
        recipientName = recipientName,
        recipientEmail = recipientEmail,
        celebrityName = celebrity.publicName,
        productName = product.name,
        orderDate = order.created.formatDayAsPlainLanguage,
        orderId = order.id.toString,
        pricePaid = totalAmountPaid.formatSimply,
        deliveredByDate = order.expectedDate.formatDayAsPlainLanguage,
        faqHowLongLink = services.consumerApp.absoluteUrl(getFAQ().url + "#how-long"),
        hasPrintOrder = maybePrintOrder.isDefined
      )
    ).send()

    // Clear out the shopping cart and redirect
    services.serverSessions.celebrityStorefrontCart(celebrity.id)(request.session).emptied.save()
    Right((order, didCreateBuyer))
  }

  private def persistOrder(buyerEmail: String,
                           buyerName: String,
                           recipientEmail: String,
                           product: Product,
                           recipientName: String,
                           personalNote: Option[String],
                           desiredText: Option[String],
                           stripeTokenId: Option[String],
                           charge: Option[Charge],
                           totalAmountPaid: Money,
                           billingPostalCode: String,
                           coupon: Option[Coupon],
                           printingOption: PrintingOption,
                           shippingForm: Option[CheckoutShippingForm.Valid],
                           writtenMessageRequest: WrittenMessageRequest,
                           isDemo: Boolean,
                           celebrity: Celebrity): (Order, Customer, Customer, Option[CashTransaction], Option[PrintOrder], Boolean /*Did create buyer*/) = {
    services.dbSession.connected(TransactionSerializable) {
      // Get buyer and recipient accounts and create customer face if necessary
      val (buyer, didCreateBuyer) = {
        val maybeBuyer = services.customerStore.findByEmail(buyerEmail)
        val maybeBuyerAndDidCreateBuyer = maybeBuyer.map(buyer => (buyer, false))
        maybeBuyerAndDidCreateBuyer.getOrElse((services.customerStore.createByEmail(buyerEmail, buyerName), true))
      }

      val recipient = if (buyerEmail == recipientEmail) {
        buyer
      } else {
        services.customerStore.findOrCreateByEmail(recipientEmail, recipientName)
      }

      // Persist the Order with the Stripe charge info.
      val order = buyer.buyUnsafe(product, recipient, recipientName = recipientName, messageToCelebrity = personalNote, requestedMessage = desiredText)
        .copy(amountPaidInCurrency = product.priceInCurrency)
        .withWrittenMessageRequest(writtenMessageRequest)
        .withPaymentStatus(PaymentStatus.Charged)
        .save() 

      val maybeCashTransaction = charge.map{ c =>
	      CashTransaction(accountId = buyer.account.id,
	      orderId = Some(order.id),
	      stripeCardTokenId = stripeTokenId,
	      stripeChargeId = Some(c.id),
	      billingPostalCode = Some(billingPostalCode)
	    ).withCash(totalAmountPaid).withCashTransactionType(CashTransactionType.EgraphPurchase).save()
      }
      val maybePrintOrder = if (printingOption == PrintingOption.HighQualityPrint) {
        val shippingAddress = for (validShippingForm <- shippingForm) yield {
          // Our printing partner wants all address fields to be comma-separated, even if they are empty
          List(
            validShippingForm.name,
            validShippingForm.addressLine1,
            validShippingForm.addressLine2.getOrElse(""),
            validShippingForm.city,
            validShippingForm.state,
            validShippingForm.postalCode
          ).mkString(",")
        }
        val printOrder = PrintOrder(orderId = order.id, amountPaidInCurrency = PrintOrder.pricePerPrint, shippingAddress = shippingAddress.getOrElse("")).save() // update CashTransaction
        
        maybeCashTransaction match {
          case None => None
          case Some(cashTxn) => cashTxn.copy(printOrderId = Some(printOrder.id)).save()
        }
        Some(printOrder)
      } else {
        None
      }
      coupon.map(_.use().save())

      val finalOrder = if (isDemo) {
        order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      } else {
        order
      }

      val finalCashTransaction = maybeCashTransaction match {
        case None => None
        case Some(cashTxn) => services.cashTransactionStore.findById(cashTxn.id)
      }

      (finalOrder, buyer, recipient, finalCashTransaction, maybePrintOrder, didCreateBuyer)
    }
  }

  private def saveFailedPurchaseData(purchaseData: JsValue, errorDescription: String): FailedPurchaseData =  {
    services.dbSession.connected(TransactionSerializable) {
      FailedPurchaseData(purchaseData = purchaseData.toString, errorDescription = errorDescription.take(128 /*128 is the column width*/)).save()
    }
  }
}

object EgraphPurchaseHandler extends Logging {
    // Failure base type
  sealed abstract class PurchaseFailed(val failedPurchaseData: FailedPurchaseData)

  // Our two failure cases for if the payment vendor failed or there was insufficient inventory
  case class PurchaseFailedStripeError(override val failedPurchaseData: FailedPurchaseData, stripeException: com.stripe.exception.StripeException) extends PurchaseFailed(failedPurchaseData)
  case class PurchaseFailedInsufficientInventory(override val failedPurchaseData: FailedPurchaseData) extends PurchaseFailed(failedPurchaseData)
  case class PurchaseFailedError(override val failedPurchaseData: FailedPurchaseData) extends PurchaseFailed(failedPurchaseData)

}
