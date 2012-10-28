package controllers.website

import com.google.inject._
import models._
import models.enums._
import play.api.mvc._
import services.mail.TransactionalMail
import services.mvc.OrderConfirmationEmail
import controllers.WebsiteControllers
import services.payment.{Charge, Payment}
import sjson.json.Serializer
import services.db.{DBSession, TransactionSerializable}
import services.logging.Logging
import exception.InsufficientInventoryException
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.http.ServerSessionFactory
import java.util.Date
import java.text.SimpleDateFormat
import org.apache.commons.mail.HtmlEmail
import org.joda.money.Money
import services.http.forms.purchase.CheckoutShippingForm
import controllers.routes.WebsiteControllers.getFAQ
import services._

case class EgraphPurchaseHandlerServices @Inject() (
  mail: TransactionalMail,
  customerStore: CustomerStore,
  accountStore: AccountStore,
  dbSession: DBSession,
  payment: Payment,
  serverSessions: ServerSessionFactory,
  consumerApp: ConsumerApplication
)

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
  flash: Flash,
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
  def execute(): Result = {
    val errorOrOrder = performPurchase

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
      (successfulOrder) =>
        //A redirect to the order confirmation page
        Redirect(controllers.routes.WebsiteControllers.getOrderConfirmation(successfulOrder.id)).flashing(flash + ("orderId" -> successfulOrder.id.toString))
    )
  }

  def performPurchase(): Either[PurchaseFailed, Order] = {
    val charge = try {
      services.payment.charge(totalAmountPaid, stripeTokenId, "Egraph Order from " + buyerEmail)
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
    val (order: Order, _: Customer, _: Customer, cashTransaction: CashTransaction, maybePrintOrder: Option[_]) = try {
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
        product = product)
    } catch {
      case e: InsufficientInventoryException => {
        services.payment.refund(charge.id)
        val failedPurchaseData = saveFailedPurchaseData(
          purchaseData = purchaseData, 
          errorDescription = e.getLocalizedMessage
        )
        return Left(PurchaseFailedInsufficientInventory(failedPurchaseData))
      }
      case e: Exception => {
        services.payment.refund(charge.id)
        val failedPurchaseData = saveFailedPurchaseData(
          purchaseData = purchaseData, 
          errorDescription = e.getLocalizedMessage
        )
        return Left(PurchaseFailedError(failedPurchaseData))
      }
    }

    // If the Stripe charge and Order persistence executed successfully, send a confirmation email and redirect to a confirmation page
    OrderConfirmationEmail(
      buyerName,
      buyerEmail,
      recipientName,
      recipientEmail,
      celebrity.publicName,
      product.name,
      order.id,
      order.created,
      order.expectedDate.get,
      cashTransaction.cash,
      services.consumerApp.absoluteUrl(getFAQ().url + "#how-long"),
      maybePrintOrder.isDefined,
      services.mail
    ).send()

    // Clear out the shopping cart and redirect
    services.serverSessions.celebrityStorefrontCart(celebrity.id)(request.session).emptied.save()
    Right(order)
  }

  private def persistOrder(buyerEmail: String,
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
                           celebrity: Celebrity): (Order, Customer, Customer, CashTransaction, Option[PrintOrder]) = {
    services.dbSession.connected(TransactionSerializable) {
      // Get buyer and recipient accounts and create customer face if necessary
      val buyer = services.customerStore.findOrCreateByEmail(buyerEmail, buyerName)
      val recipient = if (buyerEmail == recipientEmail) {
        buyer
      } else {
        services.customerStore.findOrCreateByEmail(recipientEmail, recipientName)
      }

      // Persist the Order with the Stripe charge info.
      val order = buyer.buy(product, recipient, recipientName = recipientName, messageToCelebrity = personalNote, requestedMessage = desiredText)
        .copy(amountPaidInCurrency = product.priceInCurrency)
        .withWrittenMessageRequest(writtenMessageRequest)
        .withPaymentStatus(PaymentStatus.Charged)
        .save()
      val cashTransaction = CashTransaction(accountId = buyer.account.id,
        orderId = Some(order.id),
        stripeCardTokenId = Some(stripeTokenId),
        stripeChargeId = Some(charge.id),
        billingPostalCode = Some(billingPostalCode)
      ).withCash(totalAmountPaid).withCashTransactionType(CashTransactionType.EgraphPurchase).save()

      val maybePrintOrderAndCash = if (printingOption == PrintingOption.HighQualityPrint) {
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
        val printOrderTransaction = cashTransaction.copy(printOrderId = Some(printOrder.id)).save()
        Some((printOrder, printOrderTransaction))
      } else {
        None
      }

      val finalOrder = if (isDemo) {
        order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      } else {
        order
      }

      val finalCashTransaction = maybePrintOrderAndCash.map { printOrderAndCash => 
        printOrderAndCash._2
      }.getOrElse(cashTransaction)
      
      val maybePrintOrder = maybePrintOrderAndCash.map { printOrderAndCash => 
        printOrderAndCash._1
      }

      (finalOrder, buyer, recipient, finalCashTransaction, maybePrintOrder)
    }
  }

  private def saveFailedPurchaseData(purchaseData: String, errorDescription: String): FailedPurchaseData =  {
    services.dbSession.connected(TransactionSerializable) {
      FailedPurchaseData(purchaseData = purchaseData, errorDescription = errorDescription.take(128 /*128 is the column width*/)).save()
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
