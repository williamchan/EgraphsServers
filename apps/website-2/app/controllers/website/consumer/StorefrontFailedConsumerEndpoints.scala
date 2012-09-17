package controllers.website.consumer

import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import play.api.mvc.Controller
import services.mvc.ImplicitHeaderAndFooterData

private[consumer] trait StorefrontFailedConsumerEndpoints
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  //
  // Controllers
  //
  def getStorefrontNoInventory(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      views.html.frontend.celebrity_storefront_no_inventory(celeb.publicName, product.name)
    }
  }

  def getStorefrontCreditCardError(celebrityUrlSlug: String, productUrlSlug: String, creditCardMsg: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      views.html.frontend.celebrity_storefront_creditcard_error(celeb.publicName, product.name, creditCardMsg)
    }
  }

  def getStorefrontPurchaseError(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      views.html.frontend.celebrity_storefront_purchase_error(celeb.publicName, product.name)
    }
  }

}
