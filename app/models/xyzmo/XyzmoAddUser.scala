package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each XyzmoAddUser instance
 */
case class XyzmoAddUserServices @Inject()(store: XyzmoAddUserStore)

case class XyzmoAddUser(id: Long = 0,
                        celebrityId: Long = 0,
                        baseResult: String = "",
                        error: Option[String] = None,
                        errorMsg: Option[String] = None,
                        created: Timestamp = Time.defaultTimestamp,
                        updated: Timestamp = Time.defaultTimestamp,
                        services: XyzmoAddUserServices = AppConfig.instance[XyzmoAddUserServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoAddUser = {
    services.store.save(this)
  }

  def withResultBase(resultBase: com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.ResultBase): XyzmoAddUser = {
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
  override def unapplied = XyzmoAddUser.unapply(this)

}

class XyzmoAddUserStore @Inject()(schema: Schema) extends Saves[XyzmoAddUser] with SavesCreatedUpdated[XyzmoAddUser] {

  //
  // Saves[XyzmoAddUser] methods
  //
  override val table = schema.xyzmoAddUserTable

  override def defineUpdate(theOld: XyzmoAddUser, theNew: XyzmoAddUser) = {
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
  // SavesCreatedUpdated[XyzmoAddUser] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoAddUser, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


