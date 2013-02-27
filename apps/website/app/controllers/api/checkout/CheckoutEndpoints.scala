package controllers.api.checkout

import com.stripe.exception.StripeException
import models.checkout.Checkout
import models.checkout.CheckoutAdapterServices
import models.checkout.Checkout.CheckoutFailedInsufficientInventory
import models.checkout.Checkout.CheckoutFailedStripeException
import models.checkout.forms.enums.ApiError
import models.enums.CheckoutCodeType
import play.api.mvc._
import play.api.libs.json._
import services.db.TransactionSerializable
import services.http.{POSTApiControllerMethod, ControllerMethod, WithDBConnection}
import services.http.filters.HttpFilters
import services.logging.Logging

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
              confirmationResultFor(transacted)

            case Some(Left(failure)) =>
              error(s"Failed to transact checkout due to $failure: session=$sessionIdSlug, celeb=$checkoutIdSlug}")
              log(Json.stringify(checkout.formState))
              log(Json.stringify(checkout.summary))

              failure match {
                case checkoutFailedInsufficientInventory: CheckoutFailedInsufficientInventory => insufficientInventoryResult
                case CheckoutFailedStripeException(_, _, e) => stripeErrorResult(e)
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

  private def stripeErrorResult(e: StripeException) = BadRequest {
    println(s"***\n***stripe exception message: ${e.getMessage}\n***")

    transactionErrorBody(
      e.getMessage,
      "Sorry, we were unable to charge your card. Please check your card information and make sure you're using a valid card."
    )
  }

  private def insufficientInventoryResult = BadRequest {
    transactionErrorBody(
      ApiError.InsufficientInventory.name,
      "Sorry, the product you attempted to purchase has run out of inventory ."
    )
  }

  private def transactionErrorBody(cause: String, msg: String) = Json.obj {
    "errors" -> Json.obj (
      "cause" -> cause,
      "message" -> msg
    )
  }
}

object CheckoutEndpoints extends Logging
