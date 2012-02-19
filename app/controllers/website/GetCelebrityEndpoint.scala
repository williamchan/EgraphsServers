package controllers.website

import play.mvc.Controller
import services.Utils
import play.mvc.Router.ActionDefinition
import services.blobs.AccessPolicy
import services.http.CelebrityAccountRequestFilters

private[controllers] trait GetCelebrityEndpoint { this: Controller =>

  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Serves the website's Celebrity root page.
   */
  def getCelebrity = {
    celebFilters.requireCelebrityUrlSlug { celebrity =>
      val profilePhotoUrl = celebrity.profilePhoto.resizedWidth(200).getSaved(AccessPolicy.Public).url

      views.Application.html.celebrity(
        celebrity, profilePhotoUrl, celebrity.products())
    }
  }

  /** Returns the home page ActionDefinition for the provided Celebrity url slug. */
  def lookupGetCelebrity(celebrityUrlSlug: String): ActionDefinition = {
    Utils.lookupUrl(
      "WebsiteControllers.getCelebrity",
      Map("celebrityUrlSlug" -> celebrityUrlSlug)
    )
  }
}

