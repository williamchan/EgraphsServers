package controllers.website

import play.mvc.Controller
import play.data.validation._
import models._
import enums.OrderReviewStatus
import play.mvc.Scope.Flash
import services.mail.Mail
import services.{Utils, AppConfig}
import play.{Play, Logger}
import controllers.WebsiteControllers
import services.payment.{Charge, Payment}
import scala.Predef._
import sjson.json.Serializer
import services.db.{DBSession, TransactionSerializable}
import services.logging.Logging
import exception.InsufficientInventoryException
import play.mvc.results.Redirect
import services.http.{ServerSessionFactory, POSTControllerMethod, CelebrityAccountRequestFilters}
import java.text.SimpleDateFormat
import org.apache.commons.mail.HtmlEmail

trait PostBuyProductEndpoint { this: Controller =>
  import PostBuyProductEndpoint.EgraphPurchaseHandler

  protected def dbSession: DBSession
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def mail: Mail
  protected def payment: Payment
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def postController: POSTControllerMethod

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
                     isDemo: Boolean = false) = postController(openDatabase = false, doCsrfCheck = (!isDemo || !payment.isTest)) {

    Logger.info("Receiving purchase order")
    val (celebrity: Celebrity, product: Product) = validateInputs(
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      buyerName = buyerName,
      buyerEmail = buyerEmail,
      stripeTokenId = stripeTokenId)

    if (!validationErrors.isEmpty) {
      WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))

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

object PostBuyProductEndpoint extends Logging {

  private val dateFormat = new SimpleDateFormat("MMMM dd, yyyy")

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
    accountStore: AccountStore = AppConfig.instance[AccountStore],
    dbSession: DBSession = AppConfig.instance[DBSession],
    payment: Payment = AppConfig.instance[Payment],
    serverSessions: ServerSessionFactory = AppConfig.instance[ServerSessionFactory],
    isDemo: Boolean = false
  ) {
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
        "productPrice" -> product.price.getAmount
      ))

      // Attempt Stripe charge. If a credit card-related error occurred, redirect to purchase screen.
      val charge = try {
        payment.charge(product.price, stripeTokenId, "Egraph Order from " + buyerEmail)
      } catch {
        case stripeException: com.stripe.exception.InvalidRequestException => {
          saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = "Credit card issue.")
          Validation.addError("Credit card", "There was an issue with the credit card")
          return WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))
        }
      }

      // Persist the Order. This is executed in its own database transaction.
      val (order: Order, buyer: Customer, recipient: Customer) = try {
        persistOrder(buyerEmail = buyerEmail,
          buyerName = buyerName,
          recipientEmail = recipientEmail,
          recipientName = recipientName,
          personalNote = personalNote,
          desiredText = desiredText,
          stripeTokenId = stripeTokenId, isDemo = isDemo, charge = charge, celebrity = celebrity, product = product, dbSession = dbSession, accountStore = accountStore, customerStore = customerStore)
      } catch {
        case e: InsufficientInventoryException => {
          payment.refund(charge.id)
          saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = e.getLocalizedMessage)
          Validation.addError("Inventory", "Our apologies. There is no more inventory available, but your celebrity will sign more Egraphs soon.")
          return WebsiteControllers.redirectWithValidationErrors(GetCelebrityProductEndpoint.url(celebrity, product), Some(false))
        }
        case e: Exception => {
          payment.refund(charge.id)
          saveFailedPurchaseData(dbSession = dbSession, purchaseData = purchaseData, errorDescription = e.getLocalizedMessage)
          throw (e)
        }
      }

      // If the Stripe charge and Order persistence executed successfully, send a confirmation email and redirect to a confirmation page
      sendOrderConfirmationEmail(buyerName = buyerName, buyerEmail = buyerEmail, recipientName = recipientName, recipientEmail = recipientEmail, celebrity, product, order, mail)
      flash.put("orderId", order.id)

      // Clear out the shopping cart and redirect
      serverSessions.celebrityStorefrontCart(celebrity.id).emptied.save()

      import WebsiteControllers.{reverse, getOrderConfirmation}

      new Redirect(reverse(getOrderConfirmation(order.id)).url)
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
        order = order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin)
      }

      val savedOrder = order.save()

      (savedOrder, buyer, recipient)
    }
  }

  private def saveFailedPurchaseData(dbSession: DBSession, purchaseData: String, errorDescription: String) {
    dbSession.connected(TransactionSerializable) {
      FailedPurchaseData(purchaseData = purchaseData, errorDescription = errorDescription.take(128 /*128 is the column width*/)).save()
    }
  }

  private def sendOrderConfirmationEmail(buyerName: String,
                                         buyerEmail: String,
                                         recipientName: String,
                                         recipientEmail: String,
                                         celebrity: Celebrity,
                                         product: Product,
                                         order: Order,
                                         mail: Mail) {
    // TODO(wchan): emails, stupid stupid emails
    /*
    import services.Finance.TypeConversions._
    val email = new HtmlEmail()
    email.setFrom("noreply@egraphs.com", "Egraphs")
    email.addTo(buyerEmail, buyerName)
    email.setSubject("Order Confirmation")
    val emailLogoSrc = "cid:"+email.embed(Play.getFile("../../modules/frontend/public/images/email-logo.jpg"))
    val emailFacebookSrc = "cid:"+email.embed(Play.getFile("../../modules/frontend/public/images/email-facebook.jpg"))
    val emailTwitterSrc = "cid:"+email.embed(Play.getFile("../../modules/frontend/public/images/email-twitter.jpg"))
    val html = views.frontend.html.email_order_confirmation(
      buyerName = buyerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      celebrityName = celebrity.publicName.get,
      productName = product.name,
      orderDate = dateFormat.format(order.created),
      orderId = order.id.toString,
      pricePaid = order.amountPaid.formatSimply,
      deliveredyDate = dateFormat.format(order.expectedDate.get), // all new Orders have expectedDate... will turn this into Date instead of Option[Date]
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
    )
    email.setHtmlMsg(html.toString())
    email.setTextMsg("Thank you so much for purchasing an Egraph. Please find your order summary information below.")
    mail.send(email)
    */
  }
}
