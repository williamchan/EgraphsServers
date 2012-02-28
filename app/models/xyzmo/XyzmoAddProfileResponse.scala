package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub

/**
 * Services used by each XyzmoAddProfileResponse instance
 */
case class XyzmoAddProfileResponseServices @Inject()(store: XyzmoAddProfileResponseStore)

case class XyzmoAddProfileResponse(id: Long = 0,
                                   celebrityId: Long = 0,
                                   baseResult: String = "",
                                   error: Option[String] = None,
                                   errorMsg: Option[String] = None,
                                   xyzmoProfileId: Option[String] = None,
                                   created: Timestamp = Time.defaultTimestamp,
                                   updated: Timestamp = Time.defaultTimestamp,
                                   services: XyzmoAddProfileResponseServices = AppConfig.instance[XyzmoAddProfileResponseServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoAddProfileResponse = {
    services.store.save(this)
  }

  def withProfile_Add_v1Response(profile_Add_v1Response: WebServiceUserAndProfileStub.Profile_Add_v1Response): XyzmoAddProfileResponse = {
    val resultBase = profile_Add_v1Response.getProfile_Add_v1Result
    val errorInfo = resultBase.getErrorInfo
    val error = if (errorInfo != null) Some(errorInfo.getError.getValue) else None
    val errorMsg = if (errorInfo != null) Some(errorInfo.getErrorMsg) else None

    val okInfo = profile_Add_v1Response.getProfile_Add_v1Result.getOkInfo
    val profileInfo = if (okInfo != null) Some(okInfo.getProfileInfo) else None
    val xyzmoProfileId = if (profileInfo.isDefined) Some(profileInfo.get.getProfileId) else None

    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg,
      xyzmoProfileId = xyzmoProfileId
    )
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoAddProfileResponse.unapply(this)

}

class XyzmoAddProfileResponseStore @Inject()(schema: Schema) extends Saves[XyzmoAddProfileResponse] with SavesCreatedUpdated[XyzmoAddProfileResponse] {

  //
  // Saves[XyzmoAddProfileResponse] methods
  //
  override val table = schema.xyzmoAddProfileResponses

  override def defineUpdate(theOld: XyzmoAddProfileResponse, theNew: XyzmoAddProfileResponse) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.baseResult := theNew.baseResult,
      theOld.error := theNew.error,
      theOld.errorMsg := theNew.errorMsg,
      theOld.xyzmoProfileId := theNew.xyzmoProfileId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[XyzmoAddProfileResponse] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoAddProfileResponse, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


