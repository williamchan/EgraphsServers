package controllers.browser

import play.mvc.Controller

import play.data.validation._
import org.apache.commons.mail.SimpleEmail
import models._
import play.mvc.Scope.Flash
import play.mvc.results.Redirect
import services.http.CelebrityAccountRequestFilters
import services.{Mail, Utils, AppConfig}

/**
 * Serves pages relating to a particular product of a celebrity.
 */
trait PostBuyProductEndpoint { this: Controller =>
  import PostBuyProductEndpoint.EgraphPurchaseHandler
  import PostBuyProductEndpoint.alphaEmailMatcher

  protected def celebFilters: CelebrityAccountRequestFilters
  protected def mail: Mail
  protected def customerStore: CustomerStore
  protected def accountStore: AccountStore

  def postBuyProduct(recipientName: String,
          recipientEmail: String,
          buyerName: String,
          buyerEmail: String,
          stripeTokenId: String,
          desiredText: Option[String],
          personalNote: Option[String]) =
  {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celebrity, product) =>
      import Validation.{required, email}
      required("Recipient name", recipientName)
      email("Recipient E-mail address", recipientEmail)
      required("Buyer name", buyerName)
      email("Buyer E-mail address", buyerEmail)
      required("stripeTokenId", stripeTokenId)

      if (validationErrors.isEmpty) {
        // Make sure these are valid email addresses for the alpha test
        Validation.`match`(
          "Recipient e-mail address at egraphs.com or raysbaseball.com",
          recipientEmail.toLowerCase,
          alphaEmailMatcher
        )

        Validation.`match`(
          "Buyer e-mail address at egraphs.com or raysbaseball.com",
          buyerEmail.toLowerCase,
          alphaEmailMatcher
        )

        if (validationErrors.isEmpty) {
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
            mail=mail,
            customerStore=customerStore,
            accountStore=accountStore
          ).execute()
        }
      } else {
        import scala.collection.JavaConversions._

        // Redirect back to the index page, providing field errors via the flash scope.
        val fieldNames = validationErrors.map { case (fieldName, _) => fieldName }
        val errorString = fieldNames.mkString(",")

        flash += ("errors" -> errorString)

        params.allSimple().foreach { param => flash += param }

        Redirect(GetCelebrityProductEndpoint.url(celebrity, product).url, false)
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
      email.setFrom("noreply@egraphs.com", "eGraphs")
      email.addTo(accountStore.findByCustomerId(buyer.id).get.email, buyerName)
      email.setSubject("Order Confirmation")
      email.setMsg(views.Application.html.order_confirmation_email(
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
