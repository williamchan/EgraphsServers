package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import services.Time

/**
 * Services used by each XyzmoDeleteUser instance
 */
case class XyzmoDeleteUserServices @Inject()(store: XyzmoDeleteUserStore)

case class XyzmoDeleteUser(id: Long = 0,
                           enrollmentBatchId: Long = 0,
                           baseResult: String = "",
                           error: Option[String] = None,
                           errorMsg: Option[String] = None,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: XyzmoDeleteUserServices = AppConfig.instance[XyzmoDeleteUserServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoDeleteUser = {
    services.store.save(this)
  }

  def withResultBase(resultBase: com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.ResultBase): XyzmoDeleteUser = {
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
  override def unapplied = XyzmoDeleteUser.unapply(this)

}

class XyzmoDeleteUserStore @Inject()(schema: Schema) extends SavesWithLongKey[XyzmoDeleteUser] with SavesCreatedUpdated[XyzmoDeleteUser] {

  //
  // SavesWithLongKey[XyzmoDeleteUser] methods
  //
  override val table = schema.xyzmoDeleteUserTable

  override def defineUpdate(theOld: XyzmoDeleteUser, theNew: XyzmoDeleteUser) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.baseResult := theNew.baseResult,
      theOld.error := theNew.error,
      theOld.errorMsg := theNew.errorMsg,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[XyzmoDeleteUser] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoDeleteUser, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


