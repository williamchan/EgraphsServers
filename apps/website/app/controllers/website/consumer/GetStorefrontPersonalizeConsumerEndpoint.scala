package controllers.website.consumer

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.Controller
import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import services.http.forms.purchase.PurchaseFormFactory

trait GetStorefrontPersonalizeConsumerEndpoint
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def purchaseFormFactory: PurchaseFormFactory
  protected def celebFilters: CelebrityAccountRequestFilters

  def getStorefrontPersonalize(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      // If the product ID of this GET didn't match the product ID in the forms, should I update the forms
      // Or redirect the get? I should redirect it to the actually selected product
      val forms = purchaseFormFactory.formsForStorefront(celeb.id)

      for (formProductId <- forms.productIdOrChoosePhotoRedirect(celeb, product).right) yield {

      }
    }
  }
}
