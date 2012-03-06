package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
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
    val errorInfo = resultBase.getErrorInfo
    val error = if (errorInfo != null) Some(errorInfo.getError.getValue) else None
    val errorMsg = if (errorInfo != null) Some(errorInfo.getErrorMsg) else None

    val okInfo = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo
    val infoEnrollOk = if (okInfo != null) Some(okInfo.getInfoEnrollOk) else None

    val enrollResult: Option[String] = if (okInfo != null) Some(okInfo.getEnrollResult.getValue) else None
    val xyzmoProfileId: Option[String] = if (infoEnrollOk.isDefined) Some(infoEnrollOk.get.getProfileId) else None
    val nrEnrolled: Option[Int] = if (infoEnrollOk.isDefined) Some(infoEnrollOk.get.getNrEnrolled) else None
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

  private def getSignatureSampleIds(enrollDynamicProfile_v1Response: WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response): Option[String] = {
    // todo(wchan): Implement once we get rid of SignatureSample
    None
  }

  private def getRejectedSignaturesSummary(enrollDynamicProfile_v1Response: WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response): Option[String] = {
    val okInfo = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo
    val rejectedSignaturesSOAPObject = if (okInfo != null) Some(okInfo.getRejectedSignatures) else None
    val rejectedSignatureArray = if (rejectedSignaturesSOAPObject.isDefined) Some(rejectedSignaturesSOAPObject.get.getRejectedSignature) else None

    if (rejectedSignatureArray.isDefined) {
      if (rejectedSignatureArray.get != null && rejectedSignatureArray.get.size > 0) {
        var rejectedSignaturesSummary: String = ""
        for (rejectedSignature <- rejectedSignatureArray.get) {
          val index: Int = rejectedSignature.getIndex
          val reasonString: String = rejectedSignature.getReason.toString
          rejectedSignaturesSummary += index + ") " + reasonString + ". "
        }
        // rejectedSignaturesSummary is a varchar(255), and longer strings shouldn't prevent this instance from being committed successfully
        if (rejectedSignaturesSummary.length() > 255) {
          return Some(rejectedSignaturesSummary.substring(0, 255))
        }
        return Some(rejectedSignaturesSummary)
      }
    }
    None
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoEnrollDynamicProfile.unapply(this)

}

class XyzmoEnrollDynamicProfileStore @Inject()(schema: Schema) extends Saves[XyzmoEnrollDynamicProfile] with SavesCreatedUpdated[XyzmoEnrollDynamicProfile] {

  def findByEnrollmentBatch(enrollmentBatch: EnrollmentBatch): Option[XyzmoEnrollDynamicProfile] = {
    from(schema.xyzmoEnrollDynamicProfileTable)(xyzmoEnrollDynamicProfile =>
      where(xyzmoEnrollDynamicProfile.enrollmentBatchId === enrollmentBatch.id)
        select (xyzmoEnrollDynamicProfile)
    ).headOption
  }

  //
  // Saves[XyzmoEnrollDynamicProfile] methods
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


