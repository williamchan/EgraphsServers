package services.mvc

import models.frontend.storefront.{StorefrontBreadcrumb, StorefrontBreadcrumbs}
import services.http.forms.purchase.{PersonalizeForm, PurchaseForms, FormReaders, PurchaseFormFactory}
import play.mvc.Http.Request
import play.mvc.Scope
import com.google.inject.Inject
import controllers.WebsiteControllers
import models.frontend.storefront.StorefrontBreadcrumb.CrumbChoice

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
  import WebsiteControllers._
  import CrumbChoice._

  def crumbsForRequest(celebrityId: Long, celebrityUrlSlug: String, maybeProductUrlSlug: Option[String])(implicit request: Request)
  : StorefrontBreadcrumbs = {

    val forms = purchaseFormFactory.formsForStorefront(celebrityId)

    val (crumbUrls, currentCrumb) = maybeProductUrlSlug.map { productUrlSlug =>
      val crumbUrls = urls(forms, celebrityUrlSlug, productUrlSlug)
      val maybeCurrentCrumb = request.actionMethod match {
        case "getStorefrontChoosePhotoTiled" => Some(ChoosePhoto)
        case "getStorefrontChoosePhotoCarousel" => Some(ChoosePhoto)
        case "getStorefrontPersonalize" => Some(Personalize)
        case "getStorefrontReview" => Some(Review)
        case "getStorefrontCheckout" => Some(Checkout)
        case "getStorefrontFinalize" => Some(Finalize)
        case _ => None
      }

      (crumbUrls, maybeCurrentCrumb)
    }.getOrElse {
      (Map.empty, Some(ChoosePhoto))
    }

    val crumbsWithUrls = StorefrontBreadcrumbs().withUrls(crumbUrls.asInstanceOf[Map[CrumbChoice, String]])

    currentCrumb.map(crumb => crumbsWithUrls.withActive(crumb)).getOrElse {
      crumbsWithUrls
    }
  }

  def urls(purchaseForms: PurchaseForms, celebrityUrlSlug: String, productUrlSlug: String) = {
    import CrumbChoice._

    val choosePhoto = (ChoosePhoto -> reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)

    val maybePersonalizeUrl = purchaseForms.productId.map { _ =>
      (Personalize -> reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug)).url)
    }

    val maybeReviewUrl = purchaseForms.personalizeForm().map { _:Any =>
      (Review -> reverse(getStorefrontReview(celebrityUrlSlug, productUrlSlug)).url)
    }

    val maybeCheckoutUrl = purchaseForms.highQualityPrint.map { _ =>
      (Checkout -> reverse(getStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url)
    }

    val maybeFinalizeUrl = purchaseForms.billingForm().map { _ =>
      (Finalize -> reverse(getStorefrontFinalize(celebrityUrlSlug, productUrlSlug)).url)
    }
    val maybeCrumbsAndUrls = List(maybePersonalizeUrl, maybeReviewUrl, maybeCheckoutUrl, maybeFinalizeUrl).flatten
    val crumbsAndUrls = choosePhoto :: maybeCrumbsAndUrls

    crumbsAndUrls.toMap.asInstanceOf[Map[CrumbChoice, String]]
  }

}