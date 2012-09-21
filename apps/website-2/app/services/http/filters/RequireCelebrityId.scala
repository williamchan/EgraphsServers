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


// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireCelebrityId @Inject() (celebStore: CelebrityStore) {
  def apply[A](celebId: Long, parser: BodyParser[A] = parse.anyContent)(operation: CelebrityRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        celeb <- celebStore.findById(celebId)
      ) yield {
        operation(CelebrityRequest(celeb, request))
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginCelebEndpoint
      //   instead of returning  a forbidden.
      maybeResult.getOrElse(noCelebIdResult)
    }
  } 

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(operation: CelebrityRequest[A] => Result)
  : Action[A] = {
    Action(parser) { implicit request =>
      Form(single("celebrityId" -> longNumber)).bindFromRequest.fold(
        errors => noCelebIdResult,
        celebrityId => this.apply(celebrityId, parser)(operation)(request)
      )
    }
  }
  
  //
  // Private members
  //
  private val noCelebIdResult = NotFound("Valid celebrity ID was required but not provided")
}