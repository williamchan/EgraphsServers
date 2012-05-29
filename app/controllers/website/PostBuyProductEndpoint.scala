package controllers.website

import play.mvc.Controller

import play.data.validation._
import org.apache.commons.mail.SimpleEmail
import models._
import play.mvc.Scope.Flash
import services.mail.Mail
import services.{Utils, AppConfig}
import play.Logger
import controllers.WebsiteControllers
import services.http.{SecurityRequestFilters, ControllerMethod, CelebrityAccountRequestFilters}
import services.payment.{Charge, Payment}
import scala.Predef._
import sjson.json.Serializer
import services.db.{DBSession, TransactionSerializable}
import services.logging.Logging
import exception.InsufficientInventoryException
import play.mvc.results.{Forbidden, Redirect}

trait PostBuyProductEndpoint { this: Controller =>
  import PostBuyProductEndpoint.EgraphPurchaseHandler
  import PostBuyProductEndpoint.alphaEmailMatcher

  protected def dbSession: DBSession
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def securityFilters: SecurityRequestFilters
  protected def mail: Mail
  protected def payment: Payment
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
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
          isDemo: Boolean = false) = controllerMethod(openDatabase=false) {

    (isDemo, payment.isTest) match {
      case (false, _) => {
        securityFilters.checkAuthenticity {
          post(recipientName, recipientEmail, buyerName, buyerEmail, stripeTokenId, desiredText, personalNote, isDemo)
        }
      }

      case (true, true) => {
        post(recipientName, recipientEmail, buyerName, buyerEmail, stripeTokenId, desiredText, personalNote, isDemo)
      }

      case _ => new Forbidden("Cannot place order. Contact Egraphs support/engineering.")
    }
  }

  private def post(recipientName: String,
                   recipientEmail: String,
                   buyerName: String,
                   buyerEmail: String,
                   stripeTokenId: String,
                   desiredText: Option[String],
                   personalNote: Option[String],
                   isDemo: Boolean = false): Any = {

    Logger.info("Receiving purchase order")
    val (celebrity: Celebrity, product: Product) = validateInputs(recipientName, recipientEmail, buyerName, buyerEmail, stripeTokenId)
    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))
    }

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
      flash = flash,
      mail = mail,
      customerStore = customerStore,
      accountStore = accountStore,
      dbSession = dbSession,
      payment = payment,
      isDemo = isDemo
    )
    purchaseHandler.execute()
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

          (celebrity, product)
      }
    }
    (celebrity, product)
  }
}

object PostBuyProductEndpoint extends Logging {

  private[PostBuyProductEndpoint] val alphaEmailMatcher = ".*@(egraphs|raysbaseball).com|zachapter@gmail.com"

  /**
   * Performs the meat of the purchase controller's interaction with domain
   * objects. Having it as a separate case class makes it more testable.
   */
  // TODO(erem): Refactor this class to be injected
  case class EgraphPurchaseHandler(recipientName: String,
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
                                   accountStore: AccountStore = AppConfig.instance[AccountStore],
                                   dbSession: DBSession = AppConfig.instance[DBSession],
                                   payment: Payment = AppConfig.instance[Payment],
                                   isDemo: Boolean = false) {
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
        "productPrice" -> product.price
      ))

      // Attempt Stripe charge. If a credit card-related error occurred, redirect to purchase screen.
      val charge = try {
        payment.charge(product.price, stripeTokenId, "Egraph Order from " + buyerEmail)
      } catch {
        case stripeException: com.stripe.exception.InvalidRequestException => {
          log("PostBuyProductEndpoint error charging card: " + stripeException.getLocalizedMessage + " . " + purchaseData)
          Validation.addError("Credit card", "There was an issue with the credit card")
          return WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))
        }
      }

      // Persist the Order. This is executed in its own database transaction.
      val (order: Order, buyer: Customer, recipient: Customer) = try {
        persistOrder(dbSession, customerStore, buyerEmail, buyerName, recipientEmail, product, recipientName, personalNote, desiredText, stripeTokenId, charge, isDemo, accountStore, celebrity)
      } catch {
        case e: InsufficientInventoryException => {
          payment.refund(charge.id)
          log("PostBuyProductEndpoint error saving order: " + e.getLocalizedMessage + " . " + purchaseData)
          // todo(wchan): purchaseData should be stored to a customer leads table per issue #109
          Validation.addError("Inventory", "Our apologies. There is no more inventory available, but your celebrity will sign more Egraphs soon.")
          return WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))
        }
        case e: Exception => {
          payment.refund(charge.id)
          log("PostBuyProductEndpoint error: " + e.getLocalizedMessage + " . " + purchaseData)
          throw (e)
        }
      }

      // If the Stripe charge and Order persistence executed successfully, send a confirmation email and redirect to a confirmation page
      sendOrderConfirmationEmail(buyerName, buyerEmail, buyer = buyer, recipient = recipient, celebrity, product, order, mail)
      flash.put("orderId", order.id)
      new Redirect(Utils.lookupUrl("WebsiteControllers.getOrderConfirmation", Map("orderId" -> order.id.toString)).url)
    }
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
                           isDemo: Boolean,
                           accountStore: AccountStore,
                           celebrity: Celebrity): (Order, Customer, Customer) = {
    dbSession.connected(TransactionSerializable) {
      // Get buyer and recipient accounts and create customer face if necessary
      val buyer = customerStore.findOrCreateByEmail(buyerEmail, buyerName)
      val recipient = if (buyerEmail == recipientEmail) {
        buyer
      } else {
        customerStore.findOrCreateByEmail(recipientEmail, recipientName)
      }

      // Persist the Order with the Stripe charge info.
      var order = buyer.buy(product, recipient, recipientName = recipientName, messageToCelebrity = personalNote, requestedMessage = desiredText).save()
      order = order.withChargeInfo(stripeCardTokenId = stripeTokenId, stripeCharge = charge)

      if (isDemo) {
        order = order.copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue)
      }

      val savedOrder = order.save()

      (savedOrder, buyer, recipient)
    }
  }

  private def sendOrderConfirmationEmail(buyerName: String,
                                         buyerEmail: String,
                                         buyer: Customer,
                                         recipient: Customer,
                                         celebrity: Celebrity,
                                         product: Product,
                                         order: Order,
                                         mail: Mail) {
    val email = new SimpleEmail()
    email.setFrom("noreply@egraphs.com", "Egraphs")
    email.addTo(buyerEmail, buyerName)
    email.setSubject("Order Confirmation")
    email.setMsg(views.Application.email.html.order_confirmation_email(
      buyer, recipient, celebrity, product, order
    ).toString().trim())
    mail.send(email)
  }
}
