package services.mvc

import models.Celebrity
import models.frontend.storefront.{ChoosePhotoRecentEgraph, ChoosePhotoCelebrity}
import services.blobs.AccessPolicy

class CelebrityViewConversions(celeb: Celebrity) {

  def recentlyFulfilledEgraphChoosePhotoViews: Iterable[ChoosePhotoRecentEgraph] = {
    celeb.ordersRecentlyFulfilled.take(6).map { fulfilled =>
      OrderViewConversions.productOrderAndEgraphToChoosePhotoRecentEgraph(
        fulfilled.product,
        fulfilled.order,
        fulfilled.egraph
      )
    }
  }

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
}


object CelebrityViewConversions {
  implicit def celebrityAsCelebrityViewConversions(celebrity: Celebrity):CelebrityViewConversions = {
    new CelebrityViewConversions(celebrity)
  }
}