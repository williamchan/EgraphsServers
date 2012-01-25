package controllers.browser

import play.mvc.Controller
import libs.Utils
import play.mvc.Router.ActionDefinition
import libs.Blobs.AccessPolicy
import services.http.CelebrityAccountRequestFilters

/**
 * Serves the website's Celebrity root page.
 */
private[controllers] trait GetCelebrityEndpoint { this: Controller =>

  protected def celebFilters: CelebrityAccountRequestFilters

  /** Controller for a celebrity's home page. */
  def getCelebrity = {
    celebFilters.requireCelebrityUrlSlug { celebrity =>
      val profilePhotoUrl = celebrity.profilePhoto.resizedWidth(200).getSaved(AccessPolicy.Public).url

      views.Application.html.celebrity(
        celebrity, profilePhotoUrl, celebrity.products())
    }
  }

  /** Returns the home page for the provided Celebrity. */
  def lookupGetCelebrity(celebrityUrlSlug: String): ActionDefinition = {
    Utils.lookupUrl(
      "WebsiteControllers.getCelebrity",
      Map("celebrityUrlSlug" -> celebrityUrlSlug)
    )
  }
}

