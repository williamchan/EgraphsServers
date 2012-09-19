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
import play.api.mvc.Controller
import services.http.forms.FormChecks
import services.http.forms.Form
import services.http.forms.purchase.FormReaders

// TODO: PLAY20 migration. Re-enable this and replace its usage of Play 1.0 form
// validation.

/*trait PostBuyDemoProductEndpoint { this: Controller =>

  protected def dbSession: DBSession
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def transactionalMail: TransactionalMail
  protected def payment: Payment
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def postController: POSTControllerMethod  
  protected def formReaders: FormReaders

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
    // OK erem. You were updating this to use the forms API.
    formReaders.forDemoPurchase.instantiateAgainstReadable(params)
    
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

object PostBuyDemoProductEndpoint  {
  case class DemoPurchase(
    recipientName: String,
    recipientEmail: String,
    buyerName: String,
    buyerEmail: String,
    stripeTokenId: String = payment.testToken().id, // Will throw xception if payment is StripePayment. Must be StripeTestPayment or YesMaamPayment.
    desiredText: Option[String],
    personalNote: Option[String]
  )
  
  class PostBuyDemoForm(val paramsMap: Form.Readable, check: FormChecks) extends Form[DemoPurchase] {
    //
    // Field values and validations
    //
    val recipientName = field("recipientName").validatedBy { toValidate =>
      for (submission <- check.isSomeValue(toValidate).right) yield submission
    }
    
    val recipientEmail = field("recipientEmail").validatedBy { toValidate =>
      for (
        submission <- check.isSomeValue(toValidate).right;
        email <- check.isEmailAddress(submission).right
      ) yield {
        email
      }
    }
    
    val buyerName = field("buyerName").validatedBy { toValidate =>
      for (submission <- check.isSomeValue(toValidate).right) yield submission      
    }
    
    val buyerEmail = field("buyerEmail").validatedBy { toValidate =>
      for (
        submission <- check.isSomeValue(toValidate).right;
        email <- check.isEmailAddress(submission).right
      ) yield {
        email
      }
    }
    
    val stripeTokenId = field("stripeTokenId").validatedBy { toValidate =>
      for (submission <- check.isSomeValue(toValidate).right) yield submission
    }
    
    val desiredText = field("desiredText").validatedBy { toValidate =>
      Right(toValidate.headOption)
    }
    
    val personalNote = field("personalNote").validatedBy { toValidate =>
      Right(toValidate.headOption)
    }
    
    //
    // Form members
    //
    protected def formAssumingValid: DemoPurchase = {
      DemoPurchase(
        recipientName.value.get,
        recipientEmail.value.get,
        buyerName.value.get,
        buyerEmail.value.get,
        stripeTokenId.value.get,
        desiredText.value.get,
        personalNote.value.get
      )
    }
  }
}*/
