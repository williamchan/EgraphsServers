package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import services.Time

/**
 * Services used by each XyzmoAddUser instance
 */
case class XyzmoAddUserServices @Inject()(store: XyzmoAddUserStore)

case class XyzmoAddUser(id: Long = 0,
                        enrollmentBatchId: Long = 0,
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
    val (error, errorMsg) = Option(resultBase.getErrorInfo) match {
      case None => (None, None)
      case Some(errorInfo) => (Some(errorInfo.getError.getValue), Some(errorInfo.getErrorMsg.take(255)))
    }
    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoAddUser.unapply(this)

}

class XyzmoAddUserStore @Inject()(schema: Schema) extends SavesWithLongKey[XyzmoAddUser] with SavesCreatedUpdated[XyzmoAddUser] {

  //
  // SavesWithLongKey[XyzmoAddUser] methods
  //
  override val table = schema.xyzmoAddUserTable



  //
  // SavesCreatedUpdated[XyzmoAddUser] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoAddUser, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


