package controllers.website.consumer

import services.http.ControllerMethod
import play.api.mvc.Controller
import services.mvc.ImplicitHeaderAndFooterData
import services.http.filters.HttpFilters
import play.api.mvc.Action

private[consumer] trait StorefrontFailedConsumerEndpoints
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  //
  // Controllers
  //
  def getStorefrontNoInventory(celebrityUrlSlug: String, productUrlSlug: String) = {
    Action { req => NotFound }
  }

  def getStorefrontPurchaseError(celebrityUrlSlug: String, productUrlSlug: String) = {
    Action { req => NotFound }
  }

  def getStorefrontCreditCardError(celebrityUrlSlug: String, productUrlSlug: String, creditCardMsg: Option[String]=None) = {
    Action { req => NotFound }
  }



  @deprecated("Transitioned to new Checkout", "02/27/2013")
  def _getStorefrontNoInventory(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod.withForm()
  { implicit authToken =>
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        Ok(views.html.frontend.celebrity_storefront_no_inventory(celeb.publicName, product.name))
      }
    }
  }

  @deprecated("Transitioned to new Checkout", "02/27/2013")
  def _getStorefrontCreditCardError(celebrityUrlSlug: String, productUrlSlug: String, creditCardMsg: Option[String]=None) = controllerMethod.withForm()
  { implicit authToken =>
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        Ok(views.html.frontend.celebrity_storefront_creditcard_error(celeb.publicName, product.name, creditCardMsg.getOrElse("")))
      }
    }
  }

  @deprecated("Transitioned to new Checkout", "02/27/2013")
  def _getStorefrontPurchaseError(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod.withForm()
  { implicit authToken =>
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        Ok(views.html.frontend.celebrity_storefront_purchase_error(celeb.publicName, product.name))
      }
    }
  }
}
