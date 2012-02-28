package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each XyzmoAddUserResponse instance
 */
case class XyzmoAddUserResponseServices @Inject()(store: XyzmoAddUserResponseStore)

case class XyzmoAddUserResponse(id: Long = 0,
                                celebrityId: Long = 0,
                                baseResult: String = "",
                                error: Option[String] = None,
                                errorMsg: Option[String] = None,
                                created: Timestamp = Time.defaultTimestamp,
                                updated: Timestamp = Time.defaultTimestamp,
                                services: XyzmoAddUserResponseServices = AppConfig.instance[XyzmoAddUserResponseServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoAddUserResponse = {
    services.store.save(this)
  }

  def withResultBase(resultBase: com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.ResultBase): XyzmoAddUserResponse = {
    val errorInfo = resultBase.getErrorInfo
    val error = if (errorInfo != null) Some(errorInfo.getError.getValue) else None
    val errorMsg = if (errorInfo != null) Some(errorInfo.getErrorMsg) else None
    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoAddUserResponse.unapply(this)

}

class XyzmoAddUserResponseStore @Inject()(schema: Schema) extends Saves[XyzmoAddUserResponse] with SavesCreatedUpdated[XyzmoAddUserResponse] {

  //
  // Saves[XyzmoAddUserResponse] methods
  //
  override val table = schema.xyzmoAddUserResponses

  override def defineUpdate(theOld: XyzmoAddUserResponse, theNew: XyzmoAddUserResponse) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.baseResult := theNew.baseResult,
      theOld.error := theNew.error,
      theOld.errorMsg := theNew.errorMsg,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[XyzmoAddUserResponse] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoAddUserResponse, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


