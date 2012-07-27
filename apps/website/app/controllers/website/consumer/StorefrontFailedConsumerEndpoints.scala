package controllers.website.consumer

import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller
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
      views.frontend.html.celebrity_storefront_no_inventory(celeb.publicName, product.name)
    }
  }

}
