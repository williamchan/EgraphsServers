package controllers

import play.mvc.Controller
import libs.Utils

import play.data.validation._
import models.{Customer, Account, Celebrity, Product}
import org.apache.commons.mail.SimpleEmail

/**
 * Serves pages relating to a particular product of a celebrity.
 */
object CelebrityProductController extends Controller
  with DBTransaction
  with RequiresCelebrityName
  with RequiresCelebrityProductName
{
  
  def index = {
    // Get errors and param values from previous unsuccessful buy
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    val fieldDefaults = (paramName: String) => paramName match {
        case "cardNumber" => "4242424242424242"
        case "cardCvc" => "333"
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }


    // Render the page
    views.Application.html.product(celebrity, product, errorFields, fieldDefaults)
  }

  val alphaEmailMatcher = ".*@(egraphs|raysbaseball).com|zachapter@gmail.com"

  def buy(recipientName: String,
          recipientEmail: String,
          buyerName: String,
          buyerEmail: String,
          stripeTokenId: String,
          desiredText: Option[String],
          personalNote: Option[String]) =
  {
    import Validation.{required, email}
    required("Recipient name", recipientName)
    email("Recipient E-mail address", recipientEmail)
    required("Buyer name", buyerName)
    email("Buyer E-mail address", buyerEmail)
    required("stripeTokenId", stripeTokenId)

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
        product
      ).execute()
    } else {
      import scala.collection.JavaConversions._
      
      // Redirect back to the index page, providing field errors via the flash scope.
      val fieldNames = validationErrors.map { case (fieldName, _) => fieldName }
      val errorString = fieldNames.mkString(",")

      flash += ("errors" -> errorString)

      params.allSimple().foreach { param => flash += param }

      Redirect(indexUrl(celebrity, product).url, false)
    }
  }

  /**
   * Reverses the product index url for a particular celebrity and product
   */
  def indexUrl(celebrity:Celebrity, product:Product) = {
    val params: Map[String, AnyRef] = Map(
      "celebrityUrlSlug" -> celebrity.urlSlug.get,
      "productUrlSlug" -> product.urlSlug
    )

    Utils.lookupUrl("CelebrityProductController.index", params)
  }

  /**
   * Performs the meat of the purchase controller's interaction with domain
   * objects. Having it as a separate case class makes it more testable.
   */
  case class EgraphPurchaseHandler(
    recipientName: String,
    recipientEmail: String,
    buyerName: String,
    buyerEmail: String,
    stripeTokenId: String,
    desiredText: Option[String],
    personalNote: Option[String],
    celebrity: Celebrity,
    product: Product)
  {
    def execute() = {
      // Get buyer and recipient accounts and create customer face if necessary
      val buyer = Customer.findOrCreateByEmail(buyerEmail, buyerName)
      val recipient = if (buyerEmail == recipientEmail) {
        buyer
      } else {
        Customer.findOrCreateByEmail(recipientEmail)
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
      email.addTo(Account.findByCustomerId(buyer.id).get.email, buyerName)
      email.setSubject("Order Confirmation")
      email.setMsg(views.Application.html.order_confirmation_email(
        buyer, recipient, celebrity, product, chargedOrder
      ).toString().trim())
      
      libs.Mail.send(email)

      // Redirect to the order page, with orderId in flash scope
      flash.put("orderId", chargedOrder.id)
      Redirect(Utils.lookupUrl(
        "OrderConfirmationController.confirm",
        Map("orderId" -> chargedOrder.id.toString)).url
      )
    }
  }
}