package controllers.api

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.POSTApiControllerMethod
import services.TempFile
import services.video.PostVideoAssetHelper
import services.http.WithoutDBConnection
import services.db.TransactionSerializable
import play.api.mvc.Action

private[controllers] trait PostVideoAssetApiEndpoint extends PostVideoAssetHelper { this: Controller =>

  protected def postApiController: POSTApiControllerMethod

  /**
   * Posts a video asset from a celebrity.
   */
  def postVideoAsset = postApiController(dbSettings = WithoutDBConnection) {
    Action(parse.multipartFormData) { implicit request =>
      val errorOrAccount = dbSession.connected(TransactionSerializable) {
        httpFilters.requireAuthenticatedAccount.filter(request)
      }

      errorOrAccount.fold(
        error => error, {
          case account =>
            postSaveVideoAssetToS3AndDBAction(request)
        })
    }
  }
}