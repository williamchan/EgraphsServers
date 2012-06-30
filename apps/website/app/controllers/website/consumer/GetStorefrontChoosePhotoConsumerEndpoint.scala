package controllers.website.consumer

import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.results.Redirect

/**
 * Endpoint for serving up the Choose Photo page
 */
private[consumer] trait GetStorefrontChoosePhotoConsumerEndpoint
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  import services.mvc.CelebrityViewConversions._
  import services.mvc.ProductViewConversions._

  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getStorefrontChoosePhotoTiled(celebrityUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityUrlSlug { celebrity =>
      val celebrityUrlSlug = celebrity.urlSlug.getOrElse("/")

      val productViews = for ((product, inventoryRemaining) <- celebrity.getActiveProductsWithInventoryRemaining()) yield {
        product.asChoosePhotoTileView(celebrityUrlSlug=celebrityUrlSlug, quantityRemaining = inventoryRemaining)
      }

      views.frontend.html.celebrity_storefront_choose_photo_tiled(
        celeb=celebrity.asChoosePhotoView,
        products=productViews,
        recentEgraphs=celebrity.recentlyFulfilledEgraphChoosePhotoViews,
        partnerIcons=List()
      )
    }
  }

  def getStorefrontChoosePhotoCarousel(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs{ (celeb, product) =>
      val products = celeb.productsInActiveInventoryBatches().toSeq
      val tiledViewLink = this.lookupGetStorefrontChoosePhotoTiled(celebrityUrlSlug).url

      products.findIndexOf(next => next.id == product.id) match {
        case -1 =>
          new Redirect(tiledViewLink)

        case indexOfProductInProductList =>
          val productViews = for (product <- products) yield {
            product.asChoosePhotoCarouselView(celebUrlSlug=celeb.urlSlug.getOrElse("/"))
          }

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

  def lookupGetStorefrontChoosePhotoTiled(celebrityUrlSlug: String) = {
    reverse(this.getStorefrontChoosePhotoTiled(celebrityUrlSlug))
  }

  def lookupGetStorefrontChoosePhotoCarousel(celebrityUrlSlug: String, productUrlSlug: String) = {
    reverse(this.getStorefrontChoosePhotoCarousel(celebrityUrlSlug: String, productUrlSlug))
  }
}

private[consumer] object GetStorefrontChoosePhotoConsumerEndpoint {
  object ChoosePhotoViewingOption {
    // Fill this with portrait and landscape
  }
}