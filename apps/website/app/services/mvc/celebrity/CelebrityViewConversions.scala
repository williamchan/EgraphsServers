package services.mvc.celebrity

import models.{ImageAsset, Celebrity}
import models.frontend.storefront.{ChoosePhotoRecentEgraph, ChoosePhotoCelebrity}
import services.blobs.AccessPolicy
import models.frontend.landing.CatalogStar
import services.Utils
import services.mvc.OrderViewConversions

/**
 * Converts Celebrities into various view models defined in the front-end module
 *
 * @param celeb the celebrity to convert.
 */
class CelebrityViewConversions(celeb: Celebrity) {

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

  /**
   * The celebrity as a CatalogStar. If some necessary data for the CatalogStar
   * were not available (e.g. publicName, storeFrontUrl) then it returns None.
   */
  def asCatalogStar(productIdsAndInventoryRemainings: Set[(Long,Int)]): CatalogStar = {
    val mastheadImageUrl = celeb
      .landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .resizedWidth(440)
      .getSaved(AccessPolicy.Public)
      .url

    val purchaseableProductsIds = productIdsAndInventoryRemainings.filter {
      productAndCount =>
        productAndCount._2 > 0
    }

    val choosePhotoUrl = Utils.lookupUrl(
      "WebsiteControllers.getStorefrontChoosePhotoTiled",
      Map("celebrityUrlSlug" -> celeb.urlSlug)
    ).url

    CatalogStar(
      name = celeb.publicName,
      secondaryText = Option(celeb.roleDescription),
      imageUrl = mastheadImageUrl,
      storefrontUrl = choosePhotoUrl,
      hasInventoryRemaining = !purchaseableProductsIds.isEmpty,
      isFeatured = celeb.isFeatured
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
