package controllers.website.consumer

import services.http.{POSTControllerMethod, ControllerMethod}
import play.api.mvc.Controller
import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.api.mvc.Results.{Redirect, Ok}
import services.http.forms.purchase.PurchaseFormFactory
import services.Utils
import models.{ProductStore, Celebrity, Product}
import controllers.WebsiteControllers
import services.mvc.celebrity.CelebrityViewConversions
import services.http.filters.HttpFilters
import play.api.mvc.Call
import play.api.mvc.Action

/**
 * Manages GET and POST of celebrity photos in the purchase flow.
 */
private[consumer] trait StorefrontChoosePhotoConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  import services.mvc.celebrity.CelebrityViewConversions._
  import services.mvc.ProductViewConversions._

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters  
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
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { celebrity =>
      Action { implicit request =>
        val celebrityUrlSlug = celebrity.urlSlug
  
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
  
        implicit def crumbs = breadcrumbData.crumbsForRequest(celebrity.id, celebrityUrlSlug, maybeProductUrlSlug)(request)
  
        val html = views.html.frontend.celebrity_storefront_choose_photo_tiled(
          celeb=celebrity.asChoosePhotoView,
          products=productViews,
          recentEgraphs=celebrity.recentlyFulfilledEgraphChoosePhotoViews,
          partnerIcons=List()
        )
        
        Ok(html)
      }
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
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        val products = celeb.productsInActiveInventoryBatches().toSeq
        val tiledViewLink = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrityUrlSlug).url
  
        products.findIndexOf(next => next.id == product.id) match {
          case -1 =>
            Redirect(tiledViewLink)
  
          case indexOfProductInProductList =>
            val productViews = for (product <- products) yield {
              product.asChoosePhotoCarouselView(celebUrlSlug=celeb.urlSlug, fbAppId = facebookAppId)
            }
  
            implicit def crumbs = breadcrumbData.crumbsForRequest(celeb.id, celebrityUrlSlug, Some(productUrlSlug))(request)
  
            Ok(views.html.frontend.celebrity_storefront_choose_photo_carousel(
              celeb = celeb.asChoosePhotoView,
              products = productViews,
              firstCarouselProductIndex=indexOfProductInProductList,
              tiledViewLink = tiledViewLink,
              recentEgraphs = celeb.recentlyFulfilledEgraphChoosePhotoViews,
              partnerIcons=List()
            ))
        }
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
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        // Save the purchase forms with the new product ID
        purchaseFormFactory.formsForStorefront(celeb.id).withProductId(product.id).save()
  
        // Redirect either to a url specified by the POST params or to the Personalize page.
        Utils.redirectToClientProvidedTarget(
          urlIfNoTarget=controllers.routes.WebsiteControllers.getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug).url
        )
      }
    }
  }
}

object StorefrontChoosePhotoConsumerEndpoints {

  def url(celebrity:Celebrity, product:Product): Call = {
    url(celebrity.urlSlug, product.urlSlug)
  }

  def url(celebrityUrlSlug: String, productUrlSlug: String): Call = {
    controllers.routes.WebsiteControllers.getStorefrontChoosePhotoCarousel(celebrityUrlSlug, productUrlSlug)
  }
}

