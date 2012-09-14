package controllers.website.nonproduction

import services.payment.Payment
import play.Logger
import models.{CustomerStore, AccountStore, Celebrity, Product}
import controllers.WebsiteControllers
import controllers.website.consumer.StorefrontChoosePhotoConsumerEndpoints
import services.db.{DBSession, TransactionSerializable}
import play.data.validation.Validation
import controllers.website.EgraphPurchaseHandler
import services.http.{POSTControllerMethod, CelebrityAccountRequestFilters}
import services.mail.TransactionalMail
import play.mvc.Controller

trait PostBuyDemoProductEndpoint { this: Controller =>

  protected def dbSession: DBSession
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def transactionalMail: TransactionalMail
  protected def payment: Payment
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def postController: POSTControllerMethod

  /**
   * For demo purposes only automatically uses test stripe APIs to pay for
   * an order specified only in domain relevant terms (recipient, buyer, etc)
   */
  def postBuyDemoProduct(recipientName: String,
                         recipientEmail: String,
                         buyerName: String,
                         buyerEmail: String,
                         stripeTokenId: String = payment.testToken().id, // Will throw xception if payment is StripePayment. Must be StripeTestPayment or YesMaamPayment.
                         desiredText: Option[String],
                         personalNote: Option[String]) = postController(openDatabase = false, doCsrfCheck = false) {

    Logger.info("Receiving purchase order")
    val (celebrity: Celebrity, product: Product) = validateInputs(
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      buyerName = buyerName,
      buyerEmail = buyerEmail,
      stripeTokenId = stripeTokenId)

    if (!validationErrors.isEmpty) {
      WebsiteControllers.redirectWithValidationErrors(StorefrontChoosePhotoConsumerEndpoints.url(celebrity, product), Some(false))

    } else {
      Logger.info("No validation errors")
      val purchaseHandler: EgraphPurchaseHandler = EgraphPurchaseHandler(
        recipientName = recipientName,
        recipientEmail = recipientEmail,
        buyerName = buyerName,
        buyerEmail = buyerEmail,
        stripeTokenId = stripeTokenId,
        desiredText = desiredText,
        personalNote = personalNote,
        celebrity = celebrity,
        product = product,
        totalAmountPaid = product.price,
        billingPostalCode = "55555",
        flash = flash,
        mail = transactionalMail,
        customerStore = customerStore,
        accountStore = accountStore,
        dbSession = dbSession,
        payment = payment,
        isDemo = true
      )
      purchaseHandler.execute()
    }
  }

  private def validateInputs(recipientName: String, recipientEmail: String, buyerName: String, buyerEmail: String, stripeTokenId: String): (Celebrity, Product) = {
    val (celebrity: Celebrity, product: Product) = dbSession.connected(TransactionSerializable) {
      celebFilters.requireCelebrityAndProductUrlSlugs {
        (celebrity, product) =>
          Logger.info("Purchase of product " + celebrity.publicName + "/" + product.name + " for " + recipientName)
          import Validation.{required, email}
          required("Recipient name", recipientName)
          required("Recipient E-mail address", recipientEmail)
          email("Recipient E-mail address", recipientEmail)
          required("Buyer name", buyerName)
          required("Buyer E-mail address", buyerEmail)
          email("Buyer E-mail address", buyerEmail)
          required("stripeTokenId", stripeTokenId)

          (celebrity, product)
      }
    }
    (celebrity, product)
  }
}
