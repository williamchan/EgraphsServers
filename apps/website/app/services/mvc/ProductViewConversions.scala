package services.mvc

import services.blobs.AccessPolicy
import models.frontend.storefront._
import models.ImageAsset.Jpeg
import controllers.WebsiteControllers
import models.{Product, LandscapeEgraphFrame, EgraphFrame, PortraitEgraphFrame}
import models.frontend.storefront.ChoosePhotoCarouselProduct
import models.frontend.storefront.ProductOrientation
import models.frontend.storefront.ChoosePhotoTileProduct

class ProductViewConversions(product: Product) {
  def asChoosePhotoTileView(celebrityUrlSlug: String=product.celebrity.urlSlug.getOrElse("/"))
  : ChoosePhotoTileProduct =
  {
    val carouselViewLink=WebsiteControllers.lookupGetStorefrontChoosePhotoCarousel(
      celebrityUrlSlug,
      product.urlSlug
    ).url

    ChoosePhotoTileProduct(
      name=product.name,
      price=product.price,
      imageUrl=productThumbnailUrl(width=340),
      targetUrl=carouselViewLink,
      quantityRemaining=product.remainingInventoryCount,
      orientation=orientationOfFrame(product.frame)
    )
  }

  def asChoosePhotoCarouselView(celebUrlSlug: String=product.celebrity.urlSlug.getOrElse("/"))
  : ChoosePhotoCarouselProduct =
  {
    ChoosePhotoCarouselProduct(
      name=product.name,
      description=product.description,
      price=product.price,
      imageUrl=productThumbnailUrl(width=575),
      personalizeLink=WebsiteControllers.lookupPostChoosePhoto(celebUrlSlug, product.urlSlug).url,
      orientation = orientationOfFrame(product.frame),
      carouselUrl=product.urlSlug,
      facebookShareLink="/", // TODO: actually pass the facebook share link
      twitterShareLink="/" // TODO: actually pass the twitter share link
    )
  }

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