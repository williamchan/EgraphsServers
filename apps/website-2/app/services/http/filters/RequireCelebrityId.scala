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
import models.{Account, Celebrity}
import play.api.libs.iteratee.{Done, Input}
import play.api.mvc.RequestHeader
import play.api.mvc.Request

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireCelebrityId @Inject() (celebStore: CelebrityStore) {

  def apply[A](celebId: Long, parser: BodyParser[A]=parse.anyContent)
    (actionFactory: Celebrity => Action[A])
    : Action[A] = 
  {
    Action(parser) { request =>
      val maybeAction = celebStore.findById(celebId).map(celeb => actionFactory(celeb).apply(request))
      
      maybeAction.getOrElse(noCelebIdResult)
    }
  }
  
  def inRequest[A](parser: BodyParser[A]=parse.anyContent)
    (actionFactory: Celebrity => Action[A])
  : Action[_] = 
  {    
    Action(parser) { implicit request =>
      Form(single("celebrityId" -> longNumber)).bindFromRequest.fold(
        errors => noCelebIdResult,
        
        celebrityId => this(celebrityId, parser)(actionFactory).apply(request)
      )
    }
  }

  def inAccount[A](account: Account, parser: BodyParser[A] = parse.anyContent)
    (actionFactory: Celebrity => Action[A])
    : Action[A] = 
  {
    Action(parser) { implicit request =>
      val maybeResult = account.celebrityId.map { celebId => 
        val action = this(celebId, parser)(actionFactory)
        
        action(request)
      }
      
      maybeResult.getOrElse(noCelebIdResult)
    }
  }
   

  //
  // Private members
  //
  private val noCelebIdResult = NotFound("Valid celebrity ID was required but not provided")
  }