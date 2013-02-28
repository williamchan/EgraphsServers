package controllers.api.checkout

import com.stripe.exception._
import models.checkout.Checkout
import Checkout._
import models.checkout.CheckoutAdapterServices

import models.checkout.forms.enums.ApiError
import models.enums.CheckoutCodeType
import play.api.mvc._
import play.api.libs.json._
import services.db.TransactionSerializable
import services.http.{POSTApiControllerMethod, ControllerMethod, WithDBConnection}
import services.http.filters.HttpFilters
import services.logging.Logging
import services.email.OrderConfirmationEmail
import models.frontend.email.OrderConfirmationEmailViewModel
import services.ConsumerApplication

/**
 * Should this be CheckoutAdapterEndpoints, maybe?
 *
 * path: /sessions/[SessionID]/checkouts/[CelebID]
 */
trait CheckoutEndpoints { this: Controller =>
  import CheckoutEndpoints._
  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def checkoutAdapters: CheckoutAdapterServices
  protected def postApiController: POSTApiControllerMethod
  protected def consumerApp: ConsumerApplication

  //
  // Controllers
  //
  /** Returns JSON representation of the checkout */
  def getCheckout(sessionIdSlug: UrlSlug, checkoutIdSlug: Long): Action[AnyContent] = {
    // Make read-only because sometimes EgraphOrderLineItemType doesn't exist for every Product.
    // We should generate them for each product and then deprecate readOnly here.
    controllerMethod(WithDBConnection(TransactionSerializable, readOnly=false)) {
      httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celebrity) =>
        Action { implicit request =>
          val maybeCheckout = checkoutAdapters.decache(celebrity.id)
          maybeCheckout map { checkout => Ok(checkout.summary) } getOrElse NotFound
        }
      }
    }
  }

  /** Returns Ok if the checkout is ready to attempt transaction */
  def postCheckout(sessionIdSlug: UrlSlug, checkoutIdSlug: Long): Action[AnyContent] = postApiController()
  {
    httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celebrity) =>
      Action { implicit request =>
        checkoutAdapters.decache(celebrity.id) map { checkout =>
          checkout.transact() match {

            case Some(Right(transacted)) =>
              checkout.cart.emptied.save()
              confirmationResultAndEmailFor(transacted)

            case Some(Left(failure)) =>
              error(s"Failed to transact checkout due to $failure: session=$sessionIdSlug, celeb=$checkoutIdSlug}")
              log(Json.stringify(checkout.formState))
              log(Json.stringify(checkout.summary))

              failure match {
                case CheckoutFailedStripeException(_, _, e: CardException) => stripeCardErrorResult(e)
                case withoutInventory: CheckoutFailedInsufficientInventory => insufficientInventoryResult
                case failedWithException: FailedCheckoutWithException => throw failedWithException.exception
                case _ => InternalServerError
              }

            case None =>
              error(s"Failed to transact checkout due to invalid state: session=$sessionIdSlug, celeb=$checkoutIdSlug}")
              log(Json.stringify(checkout.formState))
              log(Json.stringify(checkout.summary))

              BadRequest
          }
        } getOrElse {
          NotFound
        }
      }
    }
  }


  //
  // Helpers
  //
  private def confirmationResultAndEmailFor(checkout: Checkout)(implicit request: RequestHeader) = {
    import _root_.frontend.formatting.DateFormatting.Conversions._
    import controllers.routes.WebsiteControllers.getFAQ
    import models.checkout.Conversions._
    import services.Finance.TypeConversions._

    // send confirmation email
    for (orderItem <- checkout.lineItems(CheckoutCodeType.EgraphOrder).headOption) yield {
      val order = orderItem.domainObject
      val product = order.product
      val maybePrintOrder = checkout.lineItems(CheckoutCodeType.PrintOrder).headOption map (_.domainObject)
      val recipientAccount = checkout.recipientAccount getOrElse checkout.buyerAccount
      val recipientCustomer = checkout.recipientCustomer getOrElse checkout.buyerCustomer

      OrderConfirmationEmail(
        OrderConfirmationEmailViewModel(
          buyerName = checkout.buyerCustomer.name,
          buyerEmail = checkout.buyerAccount.email,
          recipientName = recipientCustomer.name,
          recipientEmail = recipientAccount.email,
          celebrityName = product.celebrity.publicName,
          celebrityGender = product.celebrity.gender,
          productName = product.name,
          orderDate = order.created.formatDayAsPlainLanguage,
          orderId = order.id.toString,
          pricePaid = order.amountPaid.formatSimply,
          deliveredByDate = order.expectedDate.formatDayAsPlainLanguage,
          faqHowLongLink = consumerApp.absoluteUrl(getFAQ().url + "#how-long"),
          maybePrintOrderShippingAddress = maybePrintOrder map (_.shippingAddress)
        )
      ).send()
    }

    // return API-specified confirmation result
    confirmationResultFor(checkout)
  }

  private def confirmationResultFor(checkout: Checkout)(implicit request: RequestHeader) = {
    import models.checkout.Conversions._
    import controllers.routes.WebsiteControllers.getOrderConfirmation
    val orderLineItem = checkout.lineItems(CheckoutCodeType.EgraphOrder).head
    val order = orderLineItem.domainObject

    val jsonResponse = Json.obj(
      "order" -> Json.obj(
        "id" -> order.id,
        "confirmationUrl" -> getOrderConfirmation(order.id).url
      )
    )
    Ok(jsonResponse).flashing(request.flash + ("orderId" -> order.id.toString))
  }

  private def stripeCardErrorResult(e: CardException) = BadRequest {
    val cause = s"stripe_${e.getCode}"
    log(s"Failure caused by ${cause}")
    transactionErrorBody("payment", cause)
  }

  private def insufficientInventoryResult = BadRequest { transactionErrorBody("egraph", ApiError.NoInventory.name) }

  private def transactionErrorBody(resource: String, cause: String) = Json.obj {
    val causeJson = Json.arr { Json.toJson(cause) }
    val resourceJson = Json.obj(resource -> causeJson)

    "errors" -> resourceJson
  }
}

object CheckoutEndpoints extends Logging
