package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import services.Time
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub

/**
 * Services used by each XyzmoAddProfile instance
 */
case class XyzmoAddProfileServices @Inject()(store: XyzmoAddProfileStore)

case class XyzmoAddProfile(id: Long = 0,
                           enrollmentBatchId: Long = 0,
                           baseResult: String = "",
                           error: Option[String] = None,
                           errorMsg: Option[String] = None,
                           xyzmoProfileId: Option[String] = None,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: XyzmoAddProfileServices = AppConfig.instance[XyzmoAddProfileServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoAddProfile = {
    services.store.save(this)
  }

  def withProfile_Add_v1Response(profile_Add_v1Response: WebServiceUserAndProfileStub.Profile_Add_v1Response): XyzmoAddProfile = {
    val resultBase = profile_Add_v1Response.getProfile_Add_v1Result

    val (error, errorMsg) = Option(resultBase.getErrorInfo) match {
      case None => (None, None)
      case Some(errorInfo) => (Some(errorInfo.getError.getValue), Some(errorInfo.getErrorMsg.take(255)))
    }

    val xyzmoProfileId = Option(profile_Add_v1Response.getProfile_Add_v1Result.getOkInfo) match {
      case None => None
      case Some(okInfo) => Option(okInfo.getProfileInfo) match {
        case None => None
        case Some(profileInfo) => Option(profileInfo.getProfileId)
      }
    }

    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg,
      xyzmoProfileId = xyzmoProfileId
    )
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoAddProfile.unapply(this)

}

class XyzmoAddProfileStore @Inject()(schema: Schema) extends SavesWithLongKey[XyzmoAddProfile] with SavesCreatedUpdated[XyzmoAddProfile] {

  //
  // SavesWithLongKey[XyzmoAddProfile] methods
  //
  override val table = schema.xyzmoAddProfileTable



  //
  // SavesCreatedUpdated[XyzmoAddProfile] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoAddProfile, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


