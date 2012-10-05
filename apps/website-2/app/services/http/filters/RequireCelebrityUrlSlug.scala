package services.http.filters

import java.util.Properties
import com.google.inject.Inject
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import play.api.mvc.Session
import services.http.SafePlayParams.Conversions._
import services.http.PlayConfig
import services.http.EgraphsSession
import models.AdministratorStore
import models.Account
import models.Celebrity
import models.CelebrityStore
import models.enums.PublishedStatus
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
      val redirectOrAction = this.asOperationResult(urlSlug, request.session)(operation)
      val redirectOrResult = redirectOrAction.right.map(action => action(request))
            
      redirectOrResult.fold(notFound => notFound, successfulResult => successfulResult)      
    }
  }
  
  def asOperationResult[A](urlSlug: String, session: Session)(operation: Celebrity => A): Either[Result, A] = {
    for (
      celeb <- celebStore.findByUrlSlug(urlSlug).toRight(left=noCelebSlugResult).right;
      viewableCeleb <- notFoundOrViewableCeleb(celeb, session).right
    ) yield {
      operation(viewableCeleb)
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
      adminId <- session.getLongOption(EgraphsSession.Key.AdminId.name);
      admin <- adminStore.findById(adminId)
    ) yield {
      true
    }
    
    maybeIsAdmin.getOrElse(false)
  }
  
}