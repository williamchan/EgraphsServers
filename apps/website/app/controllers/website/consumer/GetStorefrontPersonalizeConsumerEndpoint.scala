package controllers.website.consumer

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.Controller
import services.http.{CelebrityAccountRequestFilters, ControllerMethod}

trait GetStorefrontPersonalizeConsumerEndpoint
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getStorefrontPersonalize(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      // If the product ID of this GET didn't match the product ID in the forms, should I update the forms
      // Or redirect the get? I should redirect to the choose photo page.

      "Herp"
    }
  }
}
