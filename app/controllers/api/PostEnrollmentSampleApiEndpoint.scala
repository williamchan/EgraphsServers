package controllers.api

import models._
import play.mvc.Controller
import sjson.json.Serializer
import services.http.CelebrityAccountRequestFilters

private[controllers] trait PostEnrollmentSampleApiEndpoint { this: Controller =>
  protected def enrollmentBatchServices: EnrollmentBatchServices
  protected def celebFilters: CelebrityAccountRequestFilters

  def postEnrollmentSample(signature: Option[String], audio: Option[String], skipBiometrics: Boolean = false) = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      (signature, audio) match {
        case (Some(signatureString), Some(audioString)) =>
          val openEnrollmentBatch: Option[EnrollmentBatch] = celebrity.getOpenEnrollmentBatch()

          if (openEnrollmentBatch.isEmpty) {
            val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id, services=enrollmentBatchServices).save()
            val addEnrollmentSampleResult = enrollmentBatch.addEnrollmentSample(signatureString, audioString)
            jsonFromAddEnrollmentSampleResult(addEnrollmentSampleResult)

          } else if (!openEnrollmentBatch.get.isBatchComplete) {
            val addEnrollmentSampleResult = openEnrollmentBatch.get.addEnrollmentSample(signatureString, audioString, skipBiometrics)
            jsonFromAddEnrollmentSampleResult(addEnrollmentSampleResult)

          } else {
            // TODO(wchan): Should we reject data if this situation ever arises?
            Error("Open enrollment batch already exists and is awaiting enrollment attempt. No further enrollment samples required now.")
          }

        case _ =>
          play.Logger.info("Dismissing the invalid request")
          Error("Valid \"signature\" and \"audio\" parameters were not provided.")
      }
    }
  }

  private def jsonFromAddEnrollmentSampleResult(addEnrollmentSampleResult: (EnrollmentSample, Boolean, Int, Int)): String = {
    Serializer.SJSON.toJSON(
      Map("id" -> addEnrollmentSampleResult._1.id,
        "batch_complete" -> addEnrollmentSampleResult._2,
        "numEnrollmentSamplesInBatch" -> addEnrollmentSampleResult._3,
        "enrollmentBatchSize" -> addEnrollmentSampleResult._4,
        "enrollmentBatchId" -> addEnrollmentSampleResult._1.enrollmentBatchId
      ))
  }
}


/**
 * Controllers that handle direct API requests for celebrity resources.
 */

