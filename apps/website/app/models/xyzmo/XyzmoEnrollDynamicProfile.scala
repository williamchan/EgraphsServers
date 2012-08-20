package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import services.Time
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub

/**
 * Services used by each XyzmoEnrollDynamicProfile instance
 */
case class XyzmoEnrollDynamicProfileServices @Inject()(store: XyzmoEnrollDynamicProfileStore)

case class XyzmoEnrollDynamicProfile(id: Long = 0,
                                     enrollmentBatchId: Long = 0,
                                     baseResult: String = "",
                                     error: Option[String] = None,
                                     errorMsg: Option[String] = None,
                                     enrollResult: Option[String] = None,
                                     xyzmoProfileId: Option[String] = None,
                                     nrEnrolled: Option[Int] = None,
                                     rejectedSignaturesSummary: Option[String] = None,
                                     enrollmentSampleIds: Option[String] = None,
                                     created: Timestamp = Time.defaultTimestamp,
                                     updated: Timestamp = Time.defaultTimestamp,
                                     services: XyzmoEnrollDynamicProfileServices = AppConfig.instance[XyzmoEnrollDynamicProfileServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoEnrollDynamicProfile = {
    services.store.save(this)
  }

  def withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response: WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response): XyzmoEnrollDynamicProfile = {
    val resultBase = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result
    val (error, errorMsg) = Option(resultBase.getErrorInfo) match {
      case None => (None, None)
      case Some(errorInfo) => (Some(errorInfo.getError.getValue), Some(errorInfo.getErrorMsg.take(255)))
    }

    val (enrollResult, infoEnrollOkOption) = Option(enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo) match {
      case None => (None, None)
      case Some(okInfo) => (Option(okInfo.getEnrollResult.getValue), Option(okInfo.getInfoEnrollOk))
    }

    val (xyzmoProfileId, nrEnrolled) = infoEnrollOkOption match {
      case None => (None, None)
      case Some(infoEnrollOk) => (Some(infoEnrollOk.getProfileId), Some(infoEnrollOk.getNrEnrolled))
    }

    val rejectedSignaturesSummary: Option[String] = getRejectedSignaturesSummary(enrollDynamicProfile_v1Response)

    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg,
      enrollResult = enrollResult,
      xyzmoProfileId = xyzmoProfileId,
      nrEnrolled = nrEnrolled,
      rejectedSignaturesSummary = rejectedSignaturesSummary
    )
  }

  def isSuccessfulSignatureEnrollment: Boolean = {
    enrollResult.getOrElse(None) == WebServiceBiometricPartStub.EnrollResultEnum.EnrollCompleted.getValue
  }

  def isProfileAlreadyEnrolled: Boolean = {
    error.getOrElse(None) == WebServiceBiometricPartStub.ErrorStatus.ProfileAlreadyEnrolled.getValue
  }

  //    todo(wchan): Implement once we get rid of SignatureSample
  //  private def getSignatureSampleIds(enrollDynamicProfile_v1Response: WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response): Option[String] = {
  //    None
  //  }

  private def getRejectedSignaturesSummary(enrollDynamicProfile_v1Response: WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response): Option[String] = {
    Option(enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo) match {
      case None => None
      case Some(okInfo) => {
        Option(okInfo.getRejectedSignatures) match {
          case None => None
          case Some(arrayOfRejectedSignature) => {
            Option(arrayOfRejectedSignature.getRejectedSignature) match {
              case Some(rejectedSignatureArray) if (rejectedSignatureArray.size > 0) => {
                var rejectedSignaturesSummary: String = ""
                for (rejectedSignature <- rejectedSignatureArray) {
                  val index: Int = rejectedSignature.getIndex
                  val reasonString: String = rejectedSignature.getReason.toString
                  rejectedSignaturesSummary += index + ") " + reasonString + ". "
                }
                Some(rejectedSignaturesSummary.take(255))
              }
              case _ => None
            }
          }
        }
      }
    }
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoEnrollDynamicProfile.unapply(this)

}

class XyzmoEnrollDynamicProfileStore @Inject()(schema: Schema) extends SavesWithLongKey[XyzmoEnrollDynamicProfile] with SavesCreatedUpdated[XyzmoEnrollDynamicProfile] {

  def findByEnrollmentBatch(enrollmentBatch: EnrollmentBatch): Option[XyzmoEnrollDynamicProfile] = {
    from(schema.xyzmoEnrollDynamicProfileTable)(xyzmoEnrollDynamicProfile =>
      where(xyzmoEnrollDynamicProfile.enrollmentBatchId === enrollmentBatch.id)
        select (xyzmoEnrollDynamicProfile)
    ).headOption
  }

  //
  // SavesWithLongKey[XyzmoEnrollDynamicProfile] methods
  //
  override val table = schema.xyzmoEnrollDynamicProfileTable

  override def defineUpdate(theOld: XyzmoEnrollDynamicProfile, theNew: XyzmoEnrollDynamicProfile) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.baseResult := theNew.baseResult,
      theOld.error := theNew.error,
      theOld.errorMsg := theNew.errorMsg,
      theOld.enrollResult := theNew.enrollResult,
      theOld.xyzmoProfileId := theNew.xyzmoProfileId,
      theOld.nrEnrolled := theNew.nrEnrolled,
      theOld.rejectedSignaturesSummary := theNew.rejectedSignaturesSummary,
      theOld.enrollmentSampleIds := theNew.enrollmentSampleIds,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[XyzmoEnrollDynamicProfile] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoEnrollDynamicProfile, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


