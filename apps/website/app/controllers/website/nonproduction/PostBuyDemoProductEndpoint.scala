package controllers.website.nonproduction

import services.payment.Payment
import play.api.Logger
import models.{CustomerStore, AccountStore, Celebrity, Product}
import controllers.WebsiteControllers
import controllers.website.consumer.StorefrontChoosePhotoConsumerEndpoints
import services.db.{DBSession, TransactionSerializable}
import controllers.website.EgraphPurchaseHandler
import services.http.{POSTControllerMethod, WithoutDBConnection}
import services.mail.TransactionalMail
import play.api.mvc.Controller
import services.http.forms.FormChecks
import services.http.forms.Form
import services.http.forms.Form.Conversions._
import services.http.forms.purchase.FormReaders
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.data.{Form => PlayForm}
import play.api.data.Forms._
import services.config.ConfigFileProxy
import play.api.mvc.AnyContent

trait PostBuyDemoProductEndpoint { this: Controller =>
  import PostBuyDemoProductEndpoint.log

  protected def dbSession: DBSession
  protected def httpFilters: HttpFilters
  protected def postController: POSTControllerMethod  
  protected def formReaders: FormReaders
  protected def config: ConfigFileProxy
  protected def payment: Payment

  case class DemoPurchase(
    recipientName: String,
    recipientEmail: String,
    buyerName: String,
    buyerEmail: String,
    stripeTokenId: Option[String],
    desiredText: Option[String],
    personalNote: Option[String]
  )

  /**
   * For demo purposes only automatically uses test stripe APIs to pay for
   * an order specified only in domain relevant terms (recipient, buyer, etc)
   */
  def postBuyDemoProduct(celebrityUrlSlug: String,
                         productUrlSlug: String) = postController(doCsrfCheck = false, WithoutDBConnection) {

    log("Receiving demo purchase order")
    Action { implicit request =>
      if (!config.allowDemoPurchase) {
        NotFound
      } else {
        val errorResultOrData = validateInputs(
          celebrityUrlSlug: String,
          productUrlSlug: String
        )

        errorResultOrData.fold(
          errorResult => errorResult,

          data => {
            val (celebrity: Celebrity, product: Product, buyDemoData: DemoPurchase) = data

            Logger.info("No validation errors")
            val purchaseHandler: EgraphPurchaseHandler = EgraphPurchaseHandler(
              recipientName = buyDemoData.recipientName,
              recipientEmail = buyDemoData.recipientEmail,
              buyerName = buyDemoData.buyerName,
              buyerEmail = buyDemoData.buyerEmail,
              stripeTokenId = Some(buyDemoData.stripeTokenId.get),
              desiredText = buyDemoData.desiredText,
              personalNote = buyDemoData.personalNote,
              celebrity = celebrity,
              product = product,
              totalAmountPaid = product.price,
              billingPostalCode = "55555",
              flash = request.flash,
              isDemo = true
            )
            purchaseHandler.execute()
          }
        )
      }
    }
  }

  def validateInputs(celebrityUrlSlug: String, productUrlSlug: String)(implicit request: Request[AnyContent]) : Either[Result, (Celebrity, Product, DemoPurchase)] = {
    dbSession.connected(TransactionSerializable) {
      val resultOrResultOrData = httpFilters.requireCelebrityAndProductUrlSlugs.asOperationalResult(celebrityUrlSlug, productUrlSlug) { (celebrity, product) =>
        PlayForm(
          mapping(
            "recipientName" -> text,
            "recipientEmail" -> email,
            "buyerName" -> text,
            "buyerEmail" -> email,
            "stripeTokenId" -> optional(text), // This can be a real stripe token if provided in the session.
            "desiredText" -> optional(text),
            "personalNote" -> optional(text)
          )(DemoPurchase.apply)(DemoPurchase.unapply)
        ).bindFromRequest.fold(
          errors => {
            Left(Redirect(controllers.routes.WebsiteControllers.getStorefrontChoosePhotoCarousel(celebrityUrlSlug, productUrlSlug)))
          },

          data => {
            Logger.info("Purchase of product " + celebrity.publicName + "/" + product.name + " for " + data.recipientName)
            val buyDemoData = if(!data.stripeTokenId.isDefined) {
              data.copy(stripeTokenId = Some(payment.testToken().id)) // Will throw exception if payment is StripePayment. Must be StripeTestPayment or YesMaamPayment.
            } else {
              data
            }

            Right((celebrity, product, buyDemoData))
          }
        )
      }

      for(
        resultOrData <- resultOrResultOrData.right;
        data <- resultOrData.right
      ) yield {
        data
      }
    }
  }
}

object PostBuyDemoProductEndpoint extends services.logging.Logging