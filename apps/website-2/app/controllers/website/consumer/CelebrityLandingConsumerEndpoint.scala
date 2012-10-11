package controllers.website.consumer

import play.api._
import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.blobs.AccessPolicy
import services.Utils
import models.ImageAsset
import models.Celebrity
import models.frontend.header.HeaderData
import models.frontend.footer.FooterData
import play.api.templates.Html
import controllers.WebsiteControllers
import services.http.filters.HttpFilters
import egraphs.authtoken.AuthenticityToken

private[consumer] trait CelebrityLandingConsumerEndpoint
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters  

  def getCelebrityLanding(celebrityUrlSlug: String) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { celebrity =>
      Action { implicit request =>
        Ok(CelebrityLandingConsumerEndpoint.getCelebrityLandingHtml(celebrity))
      }
    }
  }
}

object CelebrityLandingConsumerEndpoint {

  def getCelebrityLandingHtml(celebrity: Celebrity)(implicit headerData: HeaderData, footerData: FooterData, authToken: AuthenticityToken): Html = {
    val landingPageImageUrl = celebrity.landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .getSaved(AccessPolicy.Public)
      .url

    val publicName = celebrity.publicName
    views.html.frontend.celebrity_landing(
      getStartedUrl = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug).url,
      celebrityPublicName = publicName,
      celebrityCasualName = celebrity.casualName.getOrElse(publicName),
      landingPageImageUrl = landingPageImageUrl,
      celebrityIsMale = true
    )
  }

  def url(celebrityUrlSlug: String): Call = {
    controllers.routes.WebsiteControllers.getCelebrityLanding(celebrityUrlSlug)
  }

}
