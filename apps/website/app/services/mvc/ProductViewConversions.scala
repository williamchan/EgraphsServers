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
      imageUrl=getProductThumbnailUrl(width=340),
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
  def asChoosePhotoCarouselView(celebUrlSlug: String=product.celebrity.urlSlug.getOrElse("/"), fbAppId: String)
  : ChoosePhotoCarouselProduct =
  {
    val imageWidth = product.frame match {
      case PortraitEgraphFrame => 340
      case LandscapeEgraphFrame => 575
    }
    val productThumbnailUrl = getProductThumbnailUrl(width=imageWidth)

    val carouselViewRoute=reverse(getStorefrontChoosePhotoCarousel(celebUrlSlug, product.urlSlug))
    carouselViewRoute.absolute()
    val carouselViewLink = carouselViewRoute.url

    val facebookShareLink = views.frontend.Utils.feedDialogLink(
      appId = fbAppId,
      picUrl = productThumbnailUrl,
      name= "Egraphs from " + product.celebrity.publicName.get,
      caption = "We are all fans.",
      description= "Egraphs are digital interactions, making it possible for stars to connect with fans " +
        "anywhere in the world. The fan writes to their favorite star, and the star sends back a 100% " +
        "personalized and authenticated handwritten note and audio message.",
      link = carouselViewLink
    )
    ChoosePhotoCarouselProduct(
      name=product.name,
      description=product.description,
      price=product.price,
      imageUrl=productThumbnailUrl,
      personalizeLink=reverse(postStorefrontChoosePhoto(celebUrlSlug, product.urlSlug)).url,
      orientation = orientationOfFrame(product.frame),
      carouselUrl=product.urlSlug,
      facebookShareLink=facebookShareLink,
      twitterShareText="Check it out. " + product.celebrity.publicName.get + " is signing egraphs!",
      carouselViewLink = carouselViewLink
    )
  }

  //
  // Private members
  //
  private def getProductThumbnailUrl(width: Int):String = {
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