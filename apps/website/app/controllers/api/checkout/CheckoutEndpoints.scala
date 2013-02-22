package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.{POSTApiControllerMethod, ControllerMethod}
import services.http.filters.HttpFilters
import models.checkout.{Checkout, CheckoutAdapterServices}
import models.enums.CheckoutCodeType


/**
 * Should this be CheckoutAdapterEndpoints, maybe?
 *
 * path: /sessions/[SessionID]/checkouts/[CelebID]
 */
trait CheckoutEndpoints { this: Controller =>

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
  def getCheckout(sessionIdSlug: UrlSlug, checkoutIdSlug: Long): Action[AnyContent] = controllerMethod()
  {
    httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celebrity) =>
      Action { implicit request =>
        val maybeCheckout = checkoutAdapters.decache(celebrity.id)
        maybeCheckout map { checkout => Ok(checkout.summary) } getOrElse NotFound
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
            case Some(Right(transacted)) => Ok(confirmationUrlFor(transacted))
            case Some(Left(failure)) => BadRequest("derp")
            case None => BadRequest("herp")
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
  private def confirmationUrlFor(checkout: Checkout) = {
    import models.checkout.checkout.Conversions._
    import controllers.routes.WebsiteControllers.getOrderConfirmation
    val order = checkout.lineItems(CheckoutCodeType.EgraphOrder).head

    getOrderConfirmation(order.id).url
  }

}
