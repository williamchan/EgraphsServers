package controllers.website.consumer

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.Controller
import services.http.{POSTControllerMethod, CelebrityAccountRequestFilters}
import services.http.forms.purchase.PurchaseFormFactory
import services.Utils
import controllers.WebsiteControllers

trait PostStorefrontChoosePhotoConsumerEndpoint
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  protected def postController: POSTControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def purchaseFormFactory: PurchaseFormFactory

  def postStorefrontChoosePhoto(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      // Save the forms with the new product ID
      purchaseFormFactory.formsForStorefront(celeb.id).withProductId(product.id).save()

      // Redirect either to a url specified by the POST params or to the Personalize page.
      import WebsiteControllers.{reverse, getStorefrontPersonalize}
      Utils.redirectToClientProvidedTarget(
        urlIfNoTarget=reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug)).url
      )
    }
  }
}
