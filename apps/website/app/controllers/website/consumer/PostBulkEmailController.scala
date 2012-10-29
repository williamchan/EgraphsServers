package controllers.website.consumer

import play.api.mvc.Controller
import services.http.{POSTControllerMethod, WithoutDBConnection}
import services.mvc.ImplicitHeaderAndFooterData
import services.mail.BulkMailList
import play.api.libs.json.Json.toJson
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._

private[controllers] trait PostBulkEmailController extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def bulkMailList: BulkMailList

  /**
   * Subscribe an email address to our bulkmail list
   */
  def postSubscribeEmail = postController(dbSettings = WithoutDBConnection) {
    Action { implicit request =>
      Form("email" -> email).bindFromRequest.fold(formWithErrors => {
        BadRequest("We're gonna need a valid email address")
      }, validForm => {
        // TODO: We aren't doing anything if this fails, maybe we should do something 
        // like store it somewhere that gets retried later since our bulk mailer service could be down..
        bulkMailList.subscribeNewAsync(validForm)
        Ok("subscribed")
      })
    }
  }
}
