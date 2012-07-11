package controllers.website.consumer

import services.http.{POSTControllerMethod, CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.{Router, Controller}
import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.results.Redirect
import services.http.forms.purchase.PurchaseFormFactory
import services.Utils
import controllers.WebsiteControllers.getStorefrontPersonalize
import models.{ProductStore, Celebrity, Product}

/**
 * Manages GET and POST of celebrity photos in the purchase flow.
 */
private[consumer] trait StorefrontChoosePhotoConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  import services.mvc.CelebrityViewConversions._
  import services.mvc.ProductViewConversions._

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def postController: POSTControllerMethod
  protected def purchaseFormFactory: PurchaseFormFactory
  protected def facebookAppId: String
  protected def breadcrumbData: StorefrontBreadcrumbData
  protected def productStore: ProductStore

  //
  // Controllers
  //
  /**
   * Controller that serves the "tiled" view of the celebrity storefront's Choose Photo
   * screen.
   *
   * @param celebrityUrlSlug identifies the celebrity storefront to serve.
   * @return the web page.
   */
  def getStorefrontChoosePhotoTiled(celebrityUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityUrlSlug { celebrity =>
      val celebrityUrlSlug = celebrity.urlSlug.getOrElse("/")

      val productViews = for (product <- celebrity.productsInActiveInventoryBatches()) yield {
        product.asChoosePhotoTileView(celebrityUrlSlug=celebrityUrlSlug)
      }

      val forms = purchaseFormFactory.formsForStorefront(celebrity.id)

      val maybeProductUrlSlug = for (
        productIdBeingOrdered <- forms.productId;
        product <- productStore.findById(productIdBeingOrdered)
      ) yield {
        product.urlSlug
      }


      implicit def crumbs = breadcrumbData.crumbsForRequest(celebrity.id, celebrityUrlSlug, maybeProductUrlSlug)

      views.frontend.html.celebrity_storefront_choose_photo_tiled(
        celeb=celebrity.asChoosePhotoView,
        products=productViews,
        recentEgraphs=celebrity.recentlyFulfilledEgraphChoosePhotoViews,
        partnerIcons=List()
      )
    }
  }

  /**
   * Controller that serves the "carousel" view of the celebrity storefront's Choose Photo
   * screen.
   *
   * @param celebrityUrlSlug identifies the celebrity whose storefront to serve.
   * @param productUrlSlug identifies the first product to display.
   * @return the web page.
   */
  def getStorefrontChoosePhotoCarousel(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs{ (celeb, product) =>
      val products = celeb.productsInActiveInventoryBatches().toSeq
      val tiledViewLink = this.reverse(this.getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url

      products.findIndexOf(next => next.id == product.id) match {
        case -1 =>
          new Redirect(tiledViewLink)

        case indexOfProductInProductList =>
          val productViews = for (product <- products) yield {
            product.asChoosePhotoCarouselView(celebUrlSlug=celeb.urlSlug.getOrElse("/"), fbAppId = facebookAppId)
          }

          implicit def crumbs = breadcrumbData.crumbsForRequest(celeb.id, celebrityUrlSlug, Some(productUrlSlug))

          views.frontend.html.celebrity_storefront_choose_photo_carousel(
            celeb = celeb.asChoosePhotoView,
            products = productViews,
            firstCarouselProductIndex=indexOfProductInProductList,
            tiledViewLink = tiledViewLink,
            recentEgraphs = celeb.recentlyFulfilledEgraphChoosePhotoViews,
            partnerIcons=List()
          )
      }
    }
  }

  /**
   * Controller that receives the first part of the purchase flow: photo selection.
   *
   * @param celebrityUrlSlug identifies the celebrity to purchase from
   * @param productUrlSlug identifies the photo being selected.
   * @return
   */
  def postStorefrontChoosePhoto(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      // Save the purchase forms with the new product ID
      purchaseFormFactory.formsForStorefront(celeb.id).withProductId(product.id).save()

      // Redirect either to a url specified by the POST params or to the Personalize page.
      Utils.redirectToClientProvidedTarget(
        urlIfNoTarget=reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug)).url
      )
    }
  }
}

object StorefrontChoosePhotoConsumerEndpoints {

  def url(celebrity:Celebrity, product:Product): Router.ActionDefinition = {
    url(celebrity.urlSlug.get, product.urlSlug)
  }

  def url(celebrityUrlSlug: String, productUrlSlug: String): Router.ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getStorefrontChoosePhotoCarousel",
      Map("celebrityUrlSlug" -> celebrityUrlSlug, "productUrlSlug" -> productUrlSlug)
    )
  }
}

