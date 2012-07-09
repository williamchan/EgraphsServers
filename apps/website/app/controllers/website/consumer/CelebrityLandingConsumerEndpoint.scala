package controllers.website.consumer

import play.mvc.{Router, Controller}
import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import services.mvc.ImplicitHeaderAndFooterData
import services.blobs.AccessPolicy
import services.Utils
import models.ImageAsset

private[consumer] trait CelebrityLandingConsumerEndpoint
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrityLanding(celebrityUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityUrlSlug { celebrity =>
      val landingPageImageUrl = celebrity.landingPageImage
        .withImageType(ImageAsset.Jpeg)
        .getSaved(AccessPolicy.Public)
        .url

      val publicName = celebrity.publicName.get
      views.frontend.html.celebrity_landing(
        celebrityPublicName = publicName,
        celebrityCasualName = celebrity.casualName.getOrElse(publicName),
        landingPageImageUrl = landingPageImageUrl,
        celebrityIsMale = true
      )
    }
  }
}

object CelebrityLandingConsumerEndpoint {

  def url(celebrityUrlSlug: String): Router.ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityLanding", Map("celebrityUrlSlug" -> celebrityUrlSlug))
  }

}
