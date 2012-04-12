package controllers.api

import models._
import play.mvc.Controller
import sjson.json.Serializer
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}
import actors.ProcessEnrollmentBatchMessage
import akka.actor.ActorRef
import services.db.{DBSession, TransactionSerializable}

private[controllers] trait PostEnrollmentSampleApiEndpoint { this: Controller =>
  protected def enrollmentBatchActor: ActorRef
  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def enrollmentBatchServices: EnrollmentBatchServices
  protected def celebFilters: CelebrityAccountRequestFilters

  def postEnrollmentSample(signature: Option[String], audio: Option[String], skipBiometrics: Boolean = false) =

    controllerMethod(openDatabase=false) {
      // Get result of DB transaction that processes the request
      val transactionResult = dbSession.connected(TransactionSerializable) {
        celebFilters.requireCelebrityAccount { (account, celebrity) =>
          (signature, audio) match {
            case (Some(signatureString), Some(audioString)) =>
              val openEnrollmentBatch: Option[EnrollmentBatch] = celebrity.getOpenEnrollmentBatch()

              if (openEnrollmentBatch.isEmpty) {
                val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id, services = enrollmentBatchServices).save()
                val addEnrollmentSampleResult = enrollmentBatch.addEnrollmentSample(signatureString, audioString)
                msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult)

              } else if (!openEnrollmentBatch.get.isBatchComplete) {
                val addEnrollmentSampleResult = openEnrollmentBatch.get.addEnrollmentSample(signatureString, audioString)
                msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult)

              } else {
                // TODO(wchan): Should we reject data if this situation ever arises?
                Error("Open enrollment batch already exists and is awaiting enrollment attempt. No further enrollment samples required now.")
              }

            case _ =>
              play.Logger.info("Dismissing the invalid postEnrollmentSample request")
              play.Logger.info("Signature length was " + (if (signature.isDefined) signature.get.length() else "[signature missing]"))
              play.Logger.info("Audio length was " + (if (audio.isDefined) audio.get.length() else "[audio missing]"))

              Error("Valid \"signature\" and \"audio\" parameters were not provided.")
          }
        }
      }
      transactionResult match {
        case (message: Option[ProcessEnrollmentBatchMessage], json: String) =>
          if (message.isDefined) enrollmentBatchActor ! message.get
          json

        case otherResult =>
          otherResult
      }
  }

  private def msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult: (EnrollmentSample, Boolean, Int, Int)):
  (Option[ProcessEnrollmentBatchMessage], String) = {

    val isBatchComplete = addEnrollmentSampleResult._2
    val enrollmentBatchId = addEnrollmentSampleResult._1.enrollmentBatchId

    val actorMessage = if (isBatchComplete) {
      Some(ProcessEnrollmentBatchMessage(id = enrollmentBatchId))
    } else {
      None
    }

    val json = Serializer.SJSON.toJSON(
      Map("id" -> addEnrollmentSampleResult._1.id,
        "batch_complete" -> isBatchComplete,
        "numEnrollmentSamplesInBatch" -> addEnrollmentSampleResult._3,
        "enrollmentBatchSize" -> addEnrollmentSampleResult._4,
        "enrollmentBatchId" -> enrollmentBatchId
      ))

    (actorMessage, json)
  }
}

