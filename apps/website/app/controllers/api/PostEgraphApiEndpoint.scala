package controllers.api

import org.postgresql.util.Base64

import actors.ProcessEgraphMessage
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.of
import play.api.data.Forms.optional
import play.api.data.Forms.text
import play.api.libs.json.Json.toJson
import play.api.mvc.Action
import play.api.mvc.Controller
import services.Time
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.{WithoutDBConnection, POSTApiControllerMethod}
import services.http.HttpCodes
import services.http.filters.HttpFilters
import services.http.forms.Play2FormFormatters.doubleFormat

private[controllers] trait PostEgraphApiEndpoint { this: Controller =>
  protected def egraphActor: ActorRef
  protected def dbSession: DBSession
  protected def postApiController: POSTApiControllerMethod
  protected def httpFilters: HttpFilters
  
  case class EgraphSubmission(
    signature: String, 
    message: Option[String], 
    audio: String, 
    latitude: Option[Double], 
    longitude: Option[Double],
    signedAt: String,
    skipBiometrics: Option[Boolean]
  )  
  
  /**
   * Posts a signed egraph from a celebrity.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints the json spec]] for more info
   * about the params.
   */
  def postEgraph(orderId: Long) = {
    postApiController(dbSettings = WithoutDBConnection) {
      Action { implicit request =>        
        val postForm = Form(
          mapping(
            "signature" -> nonEmptyText,
            "message" -> optional(text),
            "audio" -> nonEmptyText,
            "latitude" -> optional(of[Double]),
            "longitude" -> optional(of[Double]),            
            "signedAt" -> text,
            "skipBiometrics" -> optional(boolean)
          )(EgraphSubmission.apply)(EgraphSubmission.unapply)
        )
        
        postForm.bindFromRequest.fold(
          formWithErrors => {
            play.api.Logger.error("Dismissing the invalid postEgraph request: ")
            play.api.Logger.info("\t" + formWithErrors.errors.mkString(", "))
            new Status(HttpCodes.MalformedEgraph)
          },
          
          validForm => {            
            val failureOrSuccessResult = dbSession.connected(TransactionSerializable) {        
              for (
                account <- httpFilters.requireAuthenticatedAccount.filter(request).right;            
                celeb <- httpFilters.requireCelebrityId.asEitherInAccount(account).right;
                order <- httpFilters.requireOrderIdOfCelebrity.asEither(orderId, celeb.id).right
              ) yield {
                // validate signature for issue #104
                val message = validForm.message
  
                val savedEgraph = order.newEgraph
                  .copy(
                    latitude = validForm.latitude, 
                    longitude = validForm.longitude, 
                    signedAt = Time.timestamp(validForm.signedAt, Time.ipadDateFormat)
                  )
                  .withAssets(validForm.signature, validForm.message, Base64.decode(validForm.audio))
                  .save()
                val actorMessage = ProcessEgraphMessage(egraphId = savedEgraph.id, requestHeader = request)
                val successJsonResponse = Ok(toJson(Map("id" -> savedEgraph.id)))
                
                (actorMessage, successJsonResponse)
              }
            }
      
            // Route the result to an actor that performs further processing
            failureOrSuccessResult.fold(
              failureResult => failureResult,
              
              actorMessageAndSuccessResult => {
                val (actorMessage, successResult) = actorMessageAndSuccessResult
                egraphActor ! actorMessage
                
                successResult
              }
            )
          }
        )
      }
    }
  }
}

