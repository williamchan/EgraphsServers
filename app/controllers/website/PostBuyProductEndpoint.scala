package controllers.website

import play.mvc.Controller

import play.data.validation._
import org.apache.commons.mail.SimpleEmail
import models._
import play.mvc.Scope.Flash
import play.mvc.results.Redirect
import services.mail.Mail
import services.{Utils, AppConfig}
import play.Logger
import controllers.WebsiteControllers
import services.http.{SecurityRequestFilters, ControllerMethod, CelebrityAccountRequestFilters}

trait PostBuyProductEndpoint { this: Controller =>
  import PostBuyProductEndpoint.EgraphPurchaseHandler
  import PostBuyProductEndpoint.alphaEmailMatcher

  protected def celebFilters: CelebrityAccountRequestFilters
  protected def securityFilters: SecurityRequestFilters
  protected def mail: Mail
  protected def customerStore: CustomerStore
  protected def accountStore: AccountStore
  protected def controllerMethod: ControllerMethod

  /**
   * Posts a purchase order against a Celebrity's Product.
   *
   * @return a redirect either to the Product's page with form errors or to
   *   the order confirmation page.
   */
  def postBuyProduct(recipientName: String,
          recipientEmail: String,
          buyerName: String,
          buyerEmail: String,
          stripeTokenId: String,
          desiredText: Option[String],
          personalNote: Option[String],
          isApiCall: Boolean = false) = controllerMethod() {

    if (!isApiCall) {
      securityFilters.checkAuthenticity {
        post(recipientName, recipientEmail, buyerName, buyerEmail, stripeTokenId, desiredText, personalNote)
      }
    } else {
      post(recipientName, recipientEmail, buyerName, buyerEmail, stripeTokenId, desiredText, personalNote)
    }
  }

  def post(recipientName: String, recipientEmail: String, buyerName: String, buyerEmail: String, stripeTokenId: String, desiredText: Option[String], personalNote: Option[String]): Any = {
    Logger.info("Receiving purchase order")
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

        if (recipientEmail != null && !recipientEmail.isEmpty && accountStore.findByEmail(recipientEmail.toLowerCase).isEmpty) {
          Validation.`match`(
            "Recipient e-mail address must be a Beta celebrity or a Beta tester",
            recipientEmail.toLowerCase,
            alphaEmailMatcher
          )

        }
        if (buyerEmail != null && !buyerEmail.isEmpty && accountStore.findByEmail(buyerEmail.toLowerCase).isEmpty) {
          Validation.`match`(
            "Recipient e-mail address must be a Beta celebrity or a Beta tester",
            buyerEmail.toLowerCase,
            alphaEmailMatcher
          )
        }

        if (!validationErrors.isEmpty) {
          WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))
        } else {
          Logger.info("No validation errors")
          EgraphPurchaseHandler(
            recipientName,
            recipientEmail,
            buyerName,
            buyerEmail,
            stripeTokenId,
            desiredText,
            personalNote,
            celebrity,
            product,
            mail = mail,
            customerStore = customerStore,
            accountStore = accountStore
          ).execute()
        }
    }
  }
}

object PostBuyProductEndpoint {
  private[PostBuyProductEndpoint] val alphaEmailMatcher = ".*@(egraphs|raysbaseball).com|zachapter@gmail.com"

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
                                    flash: Flash = Flash.current(),
                                    mail: Mail = AppConfig.instance[Mail],
                                    customerStore: CustomerStore = AppConfig.instance[CustomerStore],
                                    accountStore: AccountStore = AppConfig.instance[AccountStore])
  {
    def execute() = {
      // Get buyer and recipient accounts and create customer face if necessary
      val buyer = customerStore.findOrCreateByEmail(buyerEmail, buyerName)
      val recipient = if (buyerEmail == recipientEmail) {
        buyer
      } else {
        customerStore.findOrCreateByEmail(recipientEmail)
      }

      // Buy the product, charge the card, persist the order.
      val order = buyer.buy(product, recipient).copy(
        stripeCardTokenId=Some(stripeTokenId),
        recipientName=recipientName,
        messageToCelebrity=personalNote,
        requestedMessage=desiredText
      ).save()

      val chargedOrder = order.charge.issueAndSave().order

      // Send the order email
      val email = new SimpleEmail()
      email.setFrom("noreply@egraphs.com", "Egraphs")
      email.addTo(accountStore.findByCustomerId(buyer.id).get.email, buyerName)
      email.setSubject("Order Confirmation")
      email.setMsg(views.Application.email.html.order_confirmation_email(
        buyer, recipient, celebrity, product, chargedOrder
      ).toString().trim())

      mail.send(email)

      // Redirect to the order page, with orderId in flash scope
      flash.put("orderId", chargedOrder.id)
      new Redirect(Utils.lookupUrl(
        "WebsiteControllers.getOrderConfirmation",
        Map("orderId" -> chargedOrder.id.toString)).url
      )
    }
  }
}
