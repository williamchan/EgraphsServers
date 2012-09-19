package controllers.website.consumer

import play.api._
import play.api.mvc._
import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import services.mvc.ImplicitHeaderAndFooterData
import services.blobs.AccessPolicy
import services.Utils
import models.ImageAsset
import models.Celebrity
import models.frontend.header.HeaderData
import models.frontend.footer.FooterData
import play.api.templates.Html
import controllers.WebsiteControllers

private[consumer] trait CelebrityLandingConsumerEndpoint
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrityLanding(celebrityUrlSlug: String) = Action { implicit request =>
    controllerMethod() {
      celebFilters.requireCelebrityUrlSlug { celebrity =>
        Ok(CelebrityLandingConsumerEndpoint.getCelebrityLandingHtml(celebrity))
      }
    }
  }
}

object CelebrityLandingConsumerEndpoint {

  def getCelebrityLandingHtml(celebrity: Celebrity)(implicit headerData: HeaderData, footerData: FooterData): Html = {
    val landingPageImageUrl = celebrity.landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .getSaved(AccessPolicy.Public)
      .url

    val publicName = celebrity.publicName
    views.html.frontend.celebrity_landing(
      getStartedUrl = WebsiteControllers.reverse(WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug)).url,
      celebrityPublicName = publicName,
      celebrityCasualName = celebrity.casualName.getOrElse(publicName),
      landingPageImageUrl = landingPageImageUrl,
      celebrityIsMale = true
    )
  }

  def url(celebrityUrlSlug: String): Router.ActionDefinition = {
    WebsiteControllers.reverse(WebsiteControllers.getCelebrityLanding(celebrityUrlSlug))
  }

}
