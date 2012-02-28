package models.xyzmo

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each XyzmoDeleteUserResponse instance
 */
case class XyzmoDeleteUserResponseServices @Inject()(store: XyzmoDeleteUserResponseStore)

case class XyzmoDeleteUserResponse(id: Long = 0,
                                   celebrityId: Long = 0,
                                   baseResult: String = "",
                                   error: Option[String] = None,
                                   errorMsg: Option[String] = None,
                                   created: Timestamp = Time.defaultTimestamp,
                                   updated: Timestamp = Time.defaultTimestamp,
                                   services: XyzmoDeleteUserResponseServices = AppConfig.instance[XyzmoDeleteUserResponseServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoDeleteUserResponse = {
    services.store.save(this)
  }

    def withResultBase(resultBase: com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.ResultBase): XyzmoDeleteUserResponse = {
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
  override def unapplied = XyzmoDeleteUserResponse.unapply(this)

}

class XyzmoDeleteUserResponseStore @Inject()(schema: Schema) extends Saves[XyzmoDeleteUserResponse] with SavesCreatedUpdated[XyzmoDeleteUserResponse] {

  //
  // Saves[XyzmoDeleteUserResponse] methods
  //
  override val table = schema.xyzmoDeleteUserResponses

  override def defineUpdate(theOld: XyzmoDeleteUserResponse, theNew: XyzmoDeleteUserResponse) = {
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
  // SavesCreatedUpdated[XyzmoDeleteUserResponse] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoDeleteUserResponse, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


