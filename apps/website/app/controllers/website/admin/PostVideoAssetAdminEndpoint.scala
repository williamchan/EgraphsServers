package controllers.website.admin

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.POSTControllerMethod
import services.TempFile
import services.video.PostVideoAssetHelper

trait PostVideoAssetAdminEndpoint extends PostVideoAssetHelper { this: Controller =>

  protected def postController: POSTControllerMethod
  
  /**
   * For posting a video asset using the admin tool at /admin/videoasset.
   * (does NOT require an authenticated account)
   */
  def postVideoAssetAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession(parse.multipartFormData) { case (admin, adminAccount) =>
        postVideoAssetBase
    }
  }
}