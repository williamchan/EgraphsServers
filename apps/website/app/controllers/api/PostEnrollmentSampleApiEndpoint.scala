package controllers.api

import models._
import play.mvc.Controller
import sjson.json.Serializer
import actors.ProcessEnrollmentBatchMessage
import akka.actor.ActorRef
import services.db.{DBSession, TransactionSerializable}
import play.data.validation.Required
import services.http.{HttpCodes, ControllerMethod, CelebrityAccountRequestFilters}

private[controllers] trait PostEnrollmentSampleApiEndpoint { this: Controller =>
  protected def enrollmentBatchActor: ActorRef
  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def enrollmentBatchServices: EnrollmentBatchServices
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def enrollmentBatchStore: EnrollmentBatchStore

  def postEnrollmentSample(@Required signature: String,
                           @Required audio: String,
                           skipBiometrics: Boolean = false /*todo(wchan): remove skipBiometrics parameter*/) =

    controllerMethod(openDatabase = false) {
      // Get result of DB transaction that processes the request
      val transactionResult = dbSession.connected(TransactionSerializable) {

        celebFilters.requireCelebrityAccount {
          (account, celebrity) =>

            // validate signature for issue #104

            if (validationErrors.isEmpty) {
              val openEnrollmentBatch: Option[EnrollmentBatch] = enrollmentBatchStore.getOpenEnrollmentBatch(celebrity)

              if (openEnrollmentBatch.isEmpty) {
                val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id, services = enrollmentBatchServices).save()
                val addEnrollmentSampleResult = enrollmentBatch.addEnrollmentSample(signature, audio)
                msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult)

              } else if (!openEnrollmentBatch.get.isBatchComplete) {
                val addEnrollmentSampleResult = openEnrollmentBatch.get.addEnrollmentSample(signature, audio)
                msgsFromAddEnrollmentSampleResult(addEnrollmentSampleResult)

              } else {
                // TODO(wchan): Should we reject data if this situation ever arises?
                Error("Open enrollment batch already exists and is awaiting enrollment attempt. No further enrollment samples required now.")
              }

            } else {
              play.Logger.info("Dismissing the invalid postEnrollmentSample request")
              if (Option(signature).isEmpty) play.Logger.info("Signature missing")
              if (Option(audio).isEmpty) play.Logger.info("Audio missing")
              Error(HttpCodes.MalformedEgraph, "")
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

