package services.mvc

import models.frontend.storefront.{StorefrontBreadcrumb, StorefrontBreadcrumbs}
import services.http.forms.purchase.{PersonalizeForm, PurchaseForms, FormReaders, PurchaseFormFactory}
import play.api.mvc.Request
import com.google.inject.Inject
import models.frontend.storefront.StorefrontBreadcrumb.CrumbChoice
import CrumbChoice._
import controllers.routes.WebsiteControllers.{
  getStorefrontChoosePhotoTiled,
  getStorefrontChoosePhotoCarousel,
  getStorefrontPersonalize,
  getStorefrontReview,
  getStorefrontCheckout,
  getStorefrontFinalize
}
import play.api.mvc.AnyContent

/**
 * Eventually this will carry information for populating the purchase
 * flow breadcrumbs.
 */
@Deprecated
trait ImplicitStorefrontBreadcrumbData {
  protected def purchaseFormFactory: PurchaseFormFactory

  implicit def breadcrumbs() = {
      StorefrontBreadcrumbs()
  }
}

class StorefrontBreadcrumbData @Inject()(purchaseFormFactory: PurchaseFormFactory) {
  import CrumbChoice._

  def crumbsForRequest(celebrityId: Long, celebrityUrlSlug: String, maybeProductUrlSlug: Option[String])
  (implicit request: Request[AnyContent])
  : StorefrontBreadcrumbs = {

    val forms = purchaseFormFactory.formsForStorefront(celebrityId)

    val (crumbUrls, currentCrumb) = maybeProductUrlSlug.map { productUrlSlug =>
      val crumbUrls = urls(forms, celebrityUrlSlug, productUrlSlug)
      val maybeCurrentCrumb = crumbForRequest(request.path, celebrityUrlSlug, maybeProductUrlSlug)

      (crumbUrls, maybeCurrentCrumb)
    }.getOrElse {
      (Map.empty, Some(ChoosePhoto))
    }

    val crumbsWithUrls = StorefrontBreadcrumbs().withUrls(crumbUrls.asInstanceOf[Map[CrumbChoice, String]])

    currentCrumb.map(crumb => crumbsWithUrls.withActive(crumb)).getOrElse {
      crumbsWithUrls
    }
  }
  
  def crumbForRequest(requestPath: String, celebrityUrlSlug: String, maybeProductUrlSlug: Option[String])
  : Option[CrumbChoice] = 
  {
    if (requestPath == getStorefrontChoosePhotoTiled(celebrityUrlSlug).url) {
      Some(ChoosePhoto)
    } else {
      maybeProductUrlSlug.flatMap { productUrlSlug =>
        if (requestPath == getStorefrontChoosePhotoCarousel(celebrityUrlSlug, productUrlSlug).url) {
          Some(ChoosePhoto)
        } else if (requestPath == getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug).url) {
          Some(Personalize)
        } else if (requestPath == getStorefrontReview(celebrityUrlSlug, productUrlSlug).url) {
          Some(Review)
        } else if (requestPath == getStorefrontCheckout(celebrityUrlSlug, productUrlSlug).url) {
          Some(Checkout)
        } else if (requestPath == getStorefrontFinalize(celebrityUrlSlug, productUrlSlug).url) {
          Some(Finalize)
        } else {
          None
        }
      }
    }
  }

  def urls(purchaseForms: PurchaseForms, celebrityUrlSlug: String, productUrlSlug: String) = {
    val choosePhoto = (ChoosePhoto -> getStorefrontChoosePhotoTiled(celebrityUrlSlug).url)

    val maybePersonalizeUrl = purchaseForms.productId.map { _ =>
      (Personalize -> getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug).url)
    }

    val maybeReviewUrl = purchaseForms.personalizeForm().map { _:Any =>
      (Review -> getStorefrontReview(celebrityUrlSlug, productUrlSlug).url)
    }

    val maybeCheckoutUrl = purchaseForms.highQualityPrint.map { _ =>
      (Checkout -> getStorefrontCheckout(celebrityUrlSlug, productUrlSlug).url)
    }

    val maybeFinalizeUrl = purchaseForms.billingForm().map { _ =>
      (Finalize -> getStorefrontFinalize(celebrityUrlSlug, productUrlSlug).url)
    }
    val maybeCrumbsAndUrls = List(maybePersonalizeUrl, maybeReviewUrl, maybeCheckoutUrl, maybeFinalizeUrl).flatten
    val crumbsAndUrls = choosePhoto :: maybeCrumbsAndUrls

    crumbsAndUrls.toMap.asInstanceOf[Map[CrumbChoice, String]]
  }

}
