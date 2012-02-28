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
 * Services used by each XyzmoVerifyUserResponse instance
 */
case class XyzmoVerifyUserResponseServices @Inject()(store: XyzmoVerifyUserResponseStore)

case class XyzmoVerifyUserResponse(id: Long = 0,
                                   egraphId: Long = 0,
                                   baseResult: String = "",
                                   error: Option[String] = None,
                                   errorMsg: Option[String] = None,
                                   isMatch: Option[Boolean] = None,
                                   score: Option[Int] = None,
                                   created: Timestamp = Time.defaultTimestamp,
                                   updated: Timestamp = Time.defaultTimestamp,
                                   services: XyzmoVerifyUserResponseServices = AppConfig.instance[XyzmoVerifyUserResponseServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoVerifyUserResponse = {
    services.store.save(this)
  }

  def withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response: WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1Response): XyzmoVerifyUserResponse = {
    val resultBase = verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result
    val errorInfo = resultBase.getErrorInfo
    val error = if (errorInfo != null) Some(errorInfo.getError.getValue) else None
    val errorMsg = if (errorInfo != null) Some(errorInfo.getErrorMsg) else None

    val okInfo = verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result.getOkInfo
    val isMatch = if (okInfo != null) Some(okInfo.getVerifyResult.getValue == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch.getValue) else None
    val score = if (okInfo != null) Some(okInfo.getScore) else None

    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg,
      isMatch = isMatch,
      score = score)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoVerifyUserResponse.unapply(this)

}

class XyzmoVerifyUserResponseStore @Inject()(schema: Schema) extends Saves[XyzmoVerifyUserResponse] with SavesCreatedUpdated[XyzmoVerifyUserResponse] {

  //
  // Saves[XyzmoVerifyUserResponse] methods
  //
  override val table = schema.xyzmoVerifyUserResponses

  override def defineUpdate(theOld: XyzmoVerifyUserResponse, theNew: XyzmoVerifyUserResponse) = {
    updateIs(
      theOld.egraphId := theNew.egraphId,
      theOld.baseResult := theNew.baseResult,
      theOld.error := theNew.error,
      theOld.errorMsg := theNew.errorMsg,
      theOld.isMatch := theNew.isMatch,
      theOld.score := theNew.score,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[XyzmoVerifyUserResponse] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoVerifyUserResponse, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


