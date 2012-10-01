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
  def getStorefrontNoInventory(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action {
        Ok(views.html.frontend.celebrity_storefront_no_inventory(celeb.publicName, product.name))
      }
    }
  }

  def getStorefrontCreditCardError(celebrityUrlSlug: String, productUrlSlug: String, creditCardMsg: Option[String]=None) = controllerMethod()
  {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action {
        Ok(views.html.frontend.celebrity_storefront_creditcard_error(celeb.publicName, product.name, creditCardMsg.getOrElse("")))
      }
    }
  }

  def getStorefrontPurchaseError(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action {
        Ok(views.html.frontend.celebrity_storefront_purchase_error(celeb.publicName, product.name))
      }
    }
  }
}
