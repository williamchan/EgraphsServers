package services.mvc.celebrity

import models.{InventoryQuantity, ImageAsset, Celebrity}
import models.frontend.storefront.{ChoosePhotoRecentEgraph, ChoosePhotoCelebrity}
import models.frontend.marketplace.MarketplaceCelebrity
import services.blobs.AccessPolicy
import models.frontend.landing.CatalogStar
import services.Utils
import services.mvc.OrderViewConversions
import services.mvc.celebrity._
import services.AppConfig
import controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled
import com.google.inject.{Provider, Inject}

/**
 * Converts Celebrities into various view models defined in the front-end module
 *
 * @param celeb the celebrity to convert.
 */
class CelebrityViewConversions(celeb: Celebrity) {
  private def catalogStarsQuery = AppConfig.instance[CatalogStarsQuery]

  /**
   * The celebrity's "Recent Egraphs" views as seen in the Choose Photo pages.
   */
  def recentlyFulfilledEgraphChoosePhotoViews: Iterable[ChoosePhotoRecentEgraph] = {
    celeb.ordersRecentlyFulfilled.take(6).map {
      fulfilled =>
        OrderViewConversions.productOrderAndEgraphToChoosePhotoRecentEgraph(
          fulfilled.product,
          fulfilled.order,
          fulfilled.egraph
        )
    }
  }

  /**
   * The celebrity in the view used by the Choose Photo pages
   */
  def asChoosePhotoView: ChoosePhotoCelebrity = {
    val profileUrl = celeb.profilePhoto.resizedWidth(80).getSaved(AccessPolicy.Public).url
    ChoosePhotoCelebrity(
      name = celeb.publicName,
      profileUrl = profileUrl,
      organization = celeb.organization,
      roleDescription = celeb.roleDescription,
      bio = celeb.bio,
      twitterUsername = celeb.twitterUsername
    )
  }
  
  def asMarketplaceCelebrity(minPrice: Int, maxPrice: Int, inventoryRemaining: Int) : MarketplaceCelebrity = {
    val photoUrl = catalogStarsQuery().find(c => c.id == celeb.id) match {
      case Some(celeb) => celeb.marketplaceImageUrl
      case None => ""
    }
    MarketplaceCelebrity(
      id = celeb.id,
      publicName = celeb.publicName,
      photoUrl = photoUrl,
      storefrontUrl = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrityUrlSlug = celeb.urlSlug).url,
      inventoryRemaining = inventoryRemaining,
      minPrice = minPrice,
      maxPrice = maxPrice,
      secondaryText = celeb.roleDescription
    )
  }

  /**
   * The celebrity as a CatalogStar. If some necessary data for the CatalogStar
   * were not available (e.g. publicName, storeFrontUrl) then it returns None.
   */
  def asCatalogStar(minPrice: Int, maxPrice: Int, inventoryRemaining: Int): CatalogStar = {
    val mastheadImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(440)
      .getSaved(AccessPolicy.Public).url  // expensive call

    val marketplaceImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(660)
      .getSaved(AccessPolicy.Public).url // expensive call

    val choosePhotoUrl = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrityUrlSlug = celeb.urlSlug).url

    CatalogStar(
      id = celeb.id,
      name = celeb.publicName,
      secondaryText = celeb.roleDescription,
      organization = celeb.organization,
      imageUrl = mastheadImageUrl,
      marketplaceImageUrl = marketplaceImageUrl,
      storefrontUrl = choosePhotoUrl,
      inventoryRemaining = inventoryRemaining,
      isFeatured = celeb.isFeatured,
      minPrice = minPrice,
      maxPrice = maxPrice
    )
  }
}

object CelebrityViewConversions extends CelebrityViewConverting {
  //
  // CelebrityViewConverting members
  //
  override implicit def celebrityAsCelebrityViewConversions(celebrity: Celebrity)
  : CelebrityViewConversions = {
    new CelebrityViewConversions(celebrity)
  }
}
