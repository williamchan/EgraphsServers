package services.mvc

import services.blobs.AccessPolicy
import models.frontend.storefront._
import models.ImageAsset.Jpeg
import controllers.WebsiteControllers.{reverse, getStorefrontChoosePhotoCarousel, postStorefrontChoosePhoto}
import models.{Product, LandscapeEgraphFrame, EgraphFrame, PortraitEgraphFrame}
import models.frontend.storefront.ChoosePhotoCarouselProduct
import models.frontend.storefront.ProductOrientation
import models.frontend.storefront.ChoosePhotoTileProduct

/**
 * Conversions to turn [[models.Product]]s into their view analogs.
 *
 * @param product the product to convert.
 */
class ProductViewConversions(product: Product) {
  /**
   * Renders the [[models.Product]] as a tile for [[views.frontend.html.celebrity_storefront_choose_photo_tiled]]
   * @param celebrityUrlSlug identifies the celebrity for use in generating a target url
   * @param quantityRemaining the remaining inventory before the product is "sold out".
   */
  def asChoosePhotoTileView(celebrityUrlSlug: String = product.celebrity.urlSlug.getOrElse("/"),
                            quantityRemaining: Int = product.remainingInventoryCount)
  : ChoosePhotoTileProduct =
  {
    val carouselViewLink=reverse(getStorefrontChoosePhotoCarousel(
      celebrityUrlSlug,
      product.urlSlug
    )).url

    ChoosePhotoTileProduct(
      name=product.name,
      price=product.price,
      imageUrl=productThumbnailUrl(width=340),
      targetUrl=carouselViewLink,
      quantityRemaining=quantityRemaining,
      orientation=orientationOfFrame(product.frame)
    )
  }

  /**
   * Renders the [[models.Product]] as a tile for [[views.frontend.html.celebrity_storefront_choose_photo_carousel]]
   *
   * @param celebUrlSlug identifies the celebrity for forming the link to post the product selection.
   */
  def asChoosePhotoCarouselView(celebUrlSlug: String=product.celebrity.urlSlug.getOrElse("/"))
  : ChoosePhotoCarouselProduct =
  {
    val imageWidth = product.frame match {
      case PortraitEgraphFrame => 340
      case LandscapeEgraphFrame => 575
    }

    ChoosePhotoCarouselProduct(
      name=product.name,
      description=product.description,
      price=product.price,
      imageUrl=productThumbnailUrl(width=imageWidth),
      personalizeLink=reverse(postStorefrontChoosePhoto(celebUrlSlug, product.urlSlug)).url,
      orientation = orientationOfFrame(product.frame),
      carouselUrl=product.urlSlug,
      facebookShareLink="/", // TODO: actually pass the facebook share link
      twitterShareLink="/" // TODO: actually pass the twitter share link
    )
  }

  //
  // Private members
  //
  private def productThumbnailUrl(width: Int):String = {
    product.photo.resizedWidth(width).withImageType(Jpeg).getSaved(AccessPolicy.Public).url
  }

  private def orientationOfFrame(frame: EgraphFrame):ProductOrientation = {
    frame match {
      case PortraitEgraphFrame => PortraitOrientation
      case LandscapeEgraphFrame => LandscapeOrientation
    }
  }
}

object ProductViewConversions {
  implicit def productToProductViewConversions(product: Product): ProductViewConversions = {
    new ProductViewConversions(product)
  }
}