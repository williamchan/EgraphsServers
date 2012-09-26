package services.http.filters

import com.google.inject.Inject
import models.CelebrityStore
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import services.http.CelebrityRequest
import models.Account
import services.http.PlayConfig
import models.Celebrity
import java.util.Properties
import play.api.mvc.Session
import models.enums.PublishedStatus
import services.http.SafePlayParams.Conversions._
import models.AdministratorStore
import controllers.WebsiteControllers

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireCelebrityUrlSlug @Inject() (
  celebStore: CelebrityStore,
  adminStore: AdministratorStore,
  @PlayConfig playConfig: Properties
) {
  
  /** NOTE: These are the scaladocs for the old CelebrityAccountRequestFilters.requireCelebrityUrlSlug
   * which has been replaced.
   * 
   * Prefer using celebrityUrlSlugOrNotFound
   *
   * Filters out requests that didn't provide a valid `celebrityUrlSlug` parameter.
   *
   * Calls the `continue` callback parameter with the corresponding [[models.Celebrity]] if the filter
   * passed.
   *
   *
   *
   * @param continue function to call if the request passed the filter
   * @param request the request whose params should be checked by the filter
   *
   * @return the return value of continue if the filter passed, otherwise `403-Forbidden`
   */
  def apply[A](urlSlug: String, parser: BodyParser[A] = parse.anyContent)(operation: Celebrity => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>
      val maybeResult = for (
        celeb <- celebStore.findByUrlSlug(urlSlug).toRight(left=noCelebSlugResult).right;
        viewableCeleb <- notFoundOrViewableCeleb(celeb, request.session).right
      ) yield {
        operation(viewableCeleb).apply(request)
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginCelebEndpoint
      //   instead of returning  a forbidden.
      maybeResult.fold(notFound => notFound, successfulResult => successfulResult)      
    }
  }
  
  //
  // Private members
  //
  private val noCelebSlugResult = NotFound("No celebrity exists with this url")
  
  private def notFoundOrViewableCeleb(celeb: Celebrity, session: Session)
  : Either[Result, Celebrity] = 
  {
    val celebIsPublished = celeb.publishedStatus == PublishedStatus.Published
    val viewerIsAdmin = isAdmin(session)
    
    if (celebIsPublished || viewerIsAdmin) { 
      Right(celeb)
    } else {
      Left(NotFound(celeb.publicName + "'s Egraphs profile is temporarily unavailable. Check back soon."))
    }
  }
   
  private def isAdmin(session: Session): Boolean = {
    val maybeIsAdmin = for (
      adminId <- session.getLongOption(WebsiteControllers.adminIdKey);
      admin <- adminStore.findById(adminId)
    ) yield {
      true
    }
    
    maybeIsAdmin.getOrElse(false)
  }
  
}