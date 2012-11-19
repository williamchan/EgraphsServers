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
  
  def asMarketplaceCelebrity(minPrice: Int, maxPrice: Int, soldout: Boolean) : MarketplaceCelebrity = {
    val photoUrl = catalogStarsQuery().filter(c => c.id == celeb.id).headOption match {
      case Some(celeb) => celeb.marketplaceImageUrl
      case None => ""
    }
    MarketplaceCelebrity(
      id = celeb.id,
      publicName = celeb.publicName,
      photoUrl = photoUrl,
      storefrontUrl = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrityUrlSlug = celeb.urlSlug).url,
      soldout = soldout,
      minPrice = minPrice,
      maxPrice = maxPrice,
      secondaryText = celeb.roleDescription
    )
  }

  /**
   * The celebrity as a CatalogStar. If some necessary data for the CatalogStar
   * were not available (e.g. publicName, storeFrontUrl) then it returns None.
   */
  def asCatalogStar(inventoryQuantities: Seq[InventoryQuantity]): CatalogStar = {
    val mastheadImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(440)
      .getSaved(AccessPolicy.Public).url

    val marketplaceImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(660)
      .getSaved(AccessPolicy.Public).url

    val purchaseableProductsIds = inventoryQuantities.filter {
      productAndCount => productAndCount.quantityRemaining > 0
    }
    
    val choosePhotoUrl = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrityUrlSlug = celeb.urlSlug).url

    CatalogStar(
      name = celeb.publicName,
      secondaryText = Option(celeb.roleDescription),
      imageUrl = mastheadImageUrl,
      marketplaceImageUrl = marketplaceImageUrl,
      storefrontUrl = choosePhotoUrl,
      hasInventoryRemaining = !purchaseableProductsIds.isEmpty,
      isFeatured = celeb.isFeatured,
      minPrice = 0,
      maxPrice = 0,
      id = celeb.id
    )
  }

  @deprecated("This is the old version before SER-86. Bringing this back because the new version does not work yet.", "")
  def asCatalogStar: CatalogStar = {
    val mastheadImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(440)
      .getSaved(AccessPolicy.Public)
      .url

    val marketplaceImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(660)
      .getSaved(AccessPolicy.Public).url

    val activeProductsAndInventory = celeb.getActiveProductsWithInventoryRemaining()
    val purchaseableProducts = activeProductsAndInventory.filter {
      productAndCount =>
        productAndCount._2 > 0
    }
    val prices = purchaseableProducts.map(p => p._1.priceInCurrency.toInt)
    val (min, max) = prices.isEmpty match {
      case true => (0, 0)
      case false => (prices.filter(p => p > 0).min, prices.max)
    }
      // marketplaceImageUrl = marketplaceImageUrl
    val choosePhotoUrl = getStorefrontChoosePhotoTiled(celeb.urlSlug).url

    CatalogStar(
      name = celeb.publicName,
      secondaryText = Option(celeb.roleDescription),
      imageUrl = mastheadImageUrl,
      marketplaceImageUrl = marketplaceImageUrl,
      storefrontUrl = choosePhotoUrl,
      hasInventoryRemaining = !purchaseableProducts.isEmpty,
      isFeatured = celeb.isFeatured,
      minPrice = min, 
      maxPrice = max,
      id = celeb.id
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
