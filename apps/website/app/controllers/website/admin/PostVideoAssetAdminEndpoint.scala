package controllers.website.admin

import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.WithoutDBConnection
import services.db.TransactionSerializable
import services.video.PostVideoAssetHelper
import play.api.mvc.Action

trait PostVideoAssetAdminEndpoint extends PostVideoAssetHelper { this: Controller =>

  protected def postController: POSTControllerMethod

  /**
   * For posting a video asset using the admin tool at /admin/videoasset.
   * (does NOT require an authenticated account)
   */
  def postVideoAssetAdmin = postController(dbSettings = WithoutDBConnection) {
    Action(parse.multipartFormData) { implicit request =>
      val errorOrAdminAndAccount = dbSession.connected(TransactionSerializable) {
        httpFilters.requireAdministratorLogin.filterInSession(parse.multipartFormData)
      }

      errorOrAdminAndAccount.fold(
        error => error, {
          case (admin, adminAccount) =>
            postSaveVideoAssetToS3AndDBAction(request)
        })
    }
  }
}