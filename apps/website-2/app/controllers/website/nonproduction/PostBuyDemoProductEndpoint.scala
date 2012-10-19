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

trait PostBuyDemoProductEndpoint { this: Controller =>

  protected def dbSession: DBSession
  protected def httpFilters: HttpFilters
  protected def transactionalMail: TransactionalMail
  protected def payment: Payment
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def postController: POSTControllerMethod  
  protected def formReaders: FormReaders

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

    Logger.info("Receiving purchase order")
    httpFilters.requireApplicationId.test {
      Action { implicit request =>
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
              stripeTokenId = buyDemoData.stripeTokenId.get, // this must always be set, and it should be
              desiredText = buyDemoData.desiredText,
              personalNote = buyDemoData.personalNote,
              celebrity = celebrity,
              product = product,
              totalAmountPaid = product.price,
              billingPostalCode = "55555",
              flash = request.flash,
              mail = transactionalMail,
              customerStore = customerStore,
              accountStore = accountStore,
              dbSession = dbSession,
              payment = payment,
              isDemo = true
            )
            purchaseHandler.execute()
          }
        )
      }
    }
  }

  def validateInputs[A](celebrityUrlSlug: String, productUrlSlug: String)(implicit request: Request[A]) : Either[Result, (Celebrity, Product, DemoPurchase)] = {
    dbSession.connected(TransactionSerializable) {
      val resultOrResultOrData = httpFilters.requireCelebrityAndProductUrlSlugs.asOperationResult(celebrityUrlSlug, productUrlSlug, request.session) { (celebrity, product) =>
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
