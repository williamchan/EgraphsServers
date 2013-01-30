package controllers.website.consumer

import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.blobs.AccessPolicy
import models.{CelebrityAccesskey, Celebrity, ImageAsset}
import models.frontend.header.HeaderData
import models.frontend.footer.FooterData
import play.api.templates.Html
import services.http.filters.HttpFilters
import egraphs.authtoken.AuthenticityToken
import egraphs.playutils.Gender

private[consumer] trait CelebrityLandingConsumerEndpoint
  extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters  

  def getCelebrityLanding(celebrityUrlSlug: String, accesskey: String = "") = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { maybeUnpublishedCelebrity =>
      httpFilters.requireAdministratorLogin.inSessionOrUseOtherFilter(maybeUnpublishedCelebrity)(
        otherFilter = httpFilters.requireCelebrityPublishedAccess.filter((maybeUnpublishedCelebrity, accesskey))
      ) { celebrity =>
        Action { implicit request =>
          Ok(CelebrityLandingConsumerEndpoint.getCelebrityLandingHtml(celebrity, accesskey))
        }
      }
    }
  }
}

object CelebrityLandingConsumerEndpoint {

  def getCelebrityLandingHtml(celebrity: Celebrity, accesskey: String = "")
                             (implicit headerData: HeaderData, footerData: FooterData, authToken: AuthenticityToken): Html = {
    val landingPageImageUrl = celebrity.landingPageImage
      .withImageType(ImageAsset.Jpeg)
      .getSaved(AccessPolicy.Public)
      .url

    val getStartedUrlBase = controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug).url
    val publicName = celebrity.publicName
    views.html.frontend.celebrity_landing(
      getStartedUrl = CelebrityAccesskey.urlWithAccesskey(getStartedUrlBase, accesskey),
      celebrityPublicName = publicName,
      celebrityCasualName = celebrity.casualName.getOrElse(publicName),
      landingPageImageUrl = landingPageImageUrl,
      celebrityGender = Gender.Male // this should not be hard-coded
    )
  }

  def url(celebrityUrlSlug: String): Call = {
    controllers.routes.WebsiteControllers.getCelebrityLanding(celebrityUrlSlug)
  }
}
