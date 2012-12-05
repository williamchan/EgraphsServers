package controllers.api

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.POSTApiControllerMethod
import services.TempFile
import services.video.PostVideoAssetHelper

private[controllers] trait PostVideoAssetApiEndpoint extends PostVideoAssetHelper { this: Controller =>

  protected def postApiController: POSTApiControllerMethod  
  
  /**
   * Posts a video asset from a celebrity.
   */
  def postVideoAsset = postApiController() {
    httpFilters.requireAuthenticatedAccount.inRequest(parse.multipartFormData) { account =>
      postVideoAssetBase
    }
  }
}