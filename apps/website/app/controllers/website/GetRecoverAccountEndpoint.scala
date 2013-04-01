package controllers.website

import egraphs.playutils.FlashableForm._
import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod

  def getRecoverAccount = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
  
      Ok(views.html.frontend.account_recover(
        accountRecoverForm = PostRecoverAccountEndpoint.form.bindWithFlashData(PostRecoverAccountEndpoint.formName),
        accountRecoverActionUrl = controllers.routes.WebsiteControllers.postRecoverAccount.url
      ))
    }
  }
}

object GetRecoverAccountEndpoint {

  def url() = {
      controllers.routes.WebsiteControllers.getRecoverAccount().url
  }
}
