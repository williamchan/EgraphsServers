package controllers.api

import actors.ProcessEnrollmentBatchMessage
import akka.actor.ActorRef
import models._
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.of
import play.api.data.Forms.optional
import play.api.data.Forms.text
import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.mvc.Result
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.{WithoutDBConnection, POSTApiControllerMethod}
import services.http.filters.HttpFilters
import play.api.mvc.Action
import services.http.HttpCodes
import services.logging.Logging
import services.http.BasicAuth

private[controllers] trait PostEnrollmentSampleApiEndpoint { this: Controller =>
  import PostEnrollmentSampleApiEndpoint._
  
  protected def enrollmentBatchActor: ActorRef
  protected def dbSession: DBSession
  protected def postApiController: POSTApiControllerMethod
  protected def httpFilters: HttpFilters
  protected def enrollmentBatchServices: EnrollmentBatchServices
  protected def enrollmentBatchStore: EnrollmentBatchStore

  def postEnrollmentSample = postApiController(dbSettings = WithoutDBConnection) {
    Action { implicit request =>
      val credentials = BasicAuth.Credentials(request).getOrElse {
        throw new RuntimeException("Accidentally ended up inside postEnrollmentSample sans credentials")
      }
      
      val postForm = Form(
        mapping(
          "signature" -> nonEmptyText,
          "audio" -> nonEmptyText,
          "skipBiometrics" -> optional(boolean)
        )(EnrollmentSubmission.apply)(EnrollmentSubmission.unapply)
      )

      postForm.bindFromRequest.fold(
        formWithErrors => {
          error("Enrollment sample from " + credentials.username + " was malformed")
          log("Dismissing the invalid postEnrollmentSample request")
          log("\t" + formWithErrors.errors.mkString(", "))
          
          new Status(HttpCodes.MalformedEgraph)
        },
        
        submission => {
          // Get result of DB transaction that processes the request
          val signature = submission.signature
          val audio = submission.audio
          val forbiddenOrErrorOrSuccess = dbSession.connected(TransactionSerializable) {
            for (
              account <- httpFilters.requireAuthenticatedAccount.asEither(request).right;
              celeb <- httpFilters.requireCelebrityId.asEitherInAccount(account).right
            ) yield {
              val openEnrollmentBatch = enrollmentBatchStore.getOpenEnrollmentBatch(celeb).getOrElse {
                log("Creating new enrollment batch for celebrity " + credentials.username)
                EnrollmentBatch(celebrityId = celeb.id, services = enrollmentBatchServices).save()
              }
              
              if (!openEnrollmentBatch.isBatchComplete) {
                log("Adding enrollment sample to batchId=" + openEnrollmentBatch.id + " for " + credentials.username)
                val addEnrollmentSampleResult = openEnrollmentBatch.addEnrollmentSample(
                  signature, 
                  audio
                )
                Right(msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult))
              } else {
                log("Rejecting enrollment sample for " + credentials.username + 
                    " pending complete enrollment of existing batch.")
                Left(InternalServerError("Open enrollment batch already exists and is awaiting enrollment attempt. No further enrollment samples required now."))                  
              }
            }
          }
          
          // Handle all the error cases, and in the success case shoot out a message to the actor
          val results = for (
            errorOrSuccess <- forbiddenOrErrorOrSuccess.right;
            success <- errorOrSuccess.right
          ) yield {
            val (maybeActorMessage, successResult) = success
            
            maybeActorMessage.foreach { actorMessage =>
              log("Sending complete enrollment batch " + actorMessage.id + " for " + 
                  credentials.username + " to biometrics for enrollment")
              enrollmentBatchActor ! actorMessage
            }
            
            successResult
          }
          
          results.merge
        }
      )

    }
  }

  private def msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult: (EnrollmentSample, Boolean, Int, Int)):
  (Option[ProcessEnrollmentBatchMessage], Result) = {

    val isBatchComplete = addEnrollmentSampleResult._2
    val enrollmentBatchId = addEnrollmentSampleResult._1.enrollmentBatchId

    val actorMessage = if (isBatchComplete) {
      Some(ProcessEnrollmentBatchMessage(id = enrollmentBatchId))
    } else {
      None
    }

    // Do indirect hackery upon the Map because Play's json serializer can't serialize it
    // whereas SJSON can.
    val jsonMap = Map(
      "id" -> addEnrollmentSampleResult._1.id,
      "batch_complete" -> isBatchComplete,
      "numEnrollmentSamplesInBatch" -> addEnrollmentSampleResult._3,
      "enrollmentBatchSize" -> addEnrollmentSampleResult._4,
      "enrollmentBatchId" -> enrollmentBatchId
    )
      
    val jsonString = sjson.json.Serializer.SJSON.toJSON(jsonMap)
    
    (actorMessage, Ok(Json.parse(jsonString)))
  }
}

object PostEnrollmentSampleApiEndpoint extends Logging {
  case class EnrollmentSubmission(signature: String, audio: String, skipBiometrics: Option[Boolean])
}
