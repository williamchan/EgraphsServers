package controllers.website.consumer

import play.mvc.Controller
import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import services.mvc.ImplicitHeaderAndFooterData
import services.blobs.AccessPolicy

private[consumer] trait CelebrityLandingConsumerEndpoint
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrityLanding(celebrityUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityUrlSlug { celebrity =>
      val publicName = celebrity.publicName.get
      views.frontend.html.celebrity_landing(
        celebrityPublicName = publicName,
        celebrityCasualName = celebrity.casualName.getOrElse(publicName),
        landingPageImageUrl = celebrity.landingPageImage.getSaved(AccessPolicy.Public).url,
        celebrityIsMale = true
      )
    }
  }
}
