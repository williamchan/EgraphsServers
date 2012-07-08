package services.mvc

import models.Celebrity
import models.frontend.storefront.{ChoosePhotoRecentEgraph, ChoosePhotoCelebrity}
import services.blobs.AccessPolicy
import models.frontend.landing.FeaturedStar
import services.Utils
import controllers.WebsiteControllers
import WebsiteControllers.{reverse, getStorefrontChoosePhotoTiled}

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
    celeb.ordersRecentlyFulfilled.take(6).map { fulfilled =>
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
      name=celeb.publicName.getOrElse("Anonymous"),
      profileUrl=profileUrl,
      organization=celeb.organization,
      roleDescription=celeb.roleDescription.getOrElse(""),
      bio=celeb.bio,
      twitterUsername=celeb.twitterUsername
    )
  }

  /**
   * The celebrity as a FeaturedStar. If some necessary data for the FeaturedStar
   * were not available (e.g. publicName, storeFrontUrl) then it returns None.
   *
   * @return
   */
  def asFeaturedStar: Option[FeaturedStar] = {
    for (publicName <- celeb.publicName; urlSlug <- celeb.urlSlug) yield {
      FeaturedStar(
        name = publicName,
        secondaryText = celeb.roleDescription,
        imageUrl = celeb.landingPageImage.resizedWidth(440).getSaved(AccessPolicy.Public).url,
        storefrontUrl = reverse(getStorefrontChoosePhotoTiled(urlSlug)).url
      )
    }
  }
}


object CelebrityViewConversions {
  implicit def celebrityAsCelebrityViewConversions(celebrity: Celebrity):CelebrityViewConversions = {
    new CelebrityViewConversions(celebrity)
  }
}