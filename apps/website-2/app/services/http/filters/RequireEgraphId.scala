package services.http.filters

import com.google.inject.Inject

import models.EgraphStore
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import services.http.EgraphRequest

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireEgraphId @Inject() (egraphStore: EgraphStore) {
  def apply[A](egraphId: Long, parser: BodyParser[A] = parse.anyContent)(operation: EgraphRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        egraph <- egraphStore.findById(egraphId)
      ) yield {
        operation(EgraphRequest(egraph, request))
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginEgraphEndpoint
      //   instead of returning  a forbidden.
      maybeResult.getOrElse(noEgraphIdResult)
    }
  } 

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(operation: EgraphRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { implicit request =>
      Form(single("egraphId" -> longNumber)).bindFromRequest.fold(
        errors => noEgraphIdResult,
        egraphId => this.apply(egraphId, parser)(operation)(request)
      )
    }
  }
  
  //
  // Private members
  //
  private val noEgraphIdResult = NotFound("Valid Egraph ID was required but not provided")
}
