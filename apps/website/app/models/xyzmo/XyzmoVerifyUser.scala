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
 * Services used by each XyzmoVerifyUser instance
 */
case class XyzmoVerifyUserServices @Inject()(store: XyzmoVerifyUserStore)

case class XyzmoVerifyUser(id: Long = 0,
                           egraphId: Long = 0,
                           baseResult: String = "",
                           error: Option[String] = None,
                           errorMsg: Option[String] = None,
                           isMatch: Option[Boolean] = None,
                           score: Option[Int] = None,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: XyzmoVerifyUserServices = AppConfig.instance[XyzmoVerifyUserServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): XyzmoVerifyUser = {
    services.store.save(this)
  }

  def withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response: WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1Response): XyzmoVerifyUser = {
    val resultBase = verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result

    val (error, errorMsg) = Option(resultBase.getErrorInfo) match {
      case None => (None, None)
      case Some(errorInfo) => (Some(errorInfo.getError.getValue), Some(errorInfo.getErrorMsg.take(255)))
    }

    val (isMatch, score) = Option(verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result.getOkInfo) match {
      case None => (None, None)
      case Some(okInfo) => {
        val isMatch = Some(okInfo.getVerifyResult.getValue == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch.getValue)
        val score = Some(okInfo.getScore)
        (isMatch, score)
      }
    }

    copy(baseResult = resultBase.getBaseResult.getValue,
      error = error,
      errorMsg = errorMsg,
      isMatch = isMatch,
      score = score)
  }

  def resultStr: String = {
    isMatch.getOrElse("") + " (" + score.getOrElse("") + ")"
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = XyzmoVerifyUser.unapply(this)

}

class XyzmoVerifyUserStore @Inject()(schema: Schema) extends SavesWithLongKey[XyzmoVerifyUser] with SavesCreatedUpdated[Long,XyzmoVerifyUser] {

  def findByEgraph(egraph: Egraph): Option[XyzmoVerifyUser] = {
    from(schema.xyzmoVerifyUserTable)(xyzmoVerifyUser =>
      where(xyzmoVerifyUser.egraphId === egraph.id)
        select (xyzmoVerifyUser)
    ).headOption
  }

  //
  // SavesWithLongKey[XyzmoVerifyUser] methods
  //
  override val table = schema.xyzmoVerifyUserTable

  override def defineUpdate(theOld: XyzmoVerifyUser, theNew: XyzmoVerifyUser) = {
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
  // SavesCreatedUpdated[Long,XyzmoVerifyUser] methods
  //
  override def withCreatedUpdated(toUpdate: XyzmoVerifyUser, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


