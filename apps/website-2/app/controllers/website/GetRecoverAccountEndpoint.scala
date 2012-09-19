package controllers.website

import play.api._
import play.api.mvc._
import services.Utils
import services.http.ControllerMethod

import services.mvc.ImplicitHeaderAndFooterData
import models.frontend.forms.Field
import models.frontend.account.{AccountRecoverForm => AccountRecoverFormView}
import services.http.forms.AccountRecoverFormFactory
import services.http.forms.AccountRecoverForm.Fields
import services.http.SafePlayParams.Conversions._
import controllers.WebsiteControllers

private[controllers] trait GetRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def accountRecoverForms: AccountRecoverFormFactory

  def getRecoverAccount = Action { implicit request =>
    controllerMethod() {
      val flash = request.flash
      val maybeFormData = accountRecoverForms.getFormReader.read(flash.asFormReadable).map { form =>
        AccountRecoverFormView(
          form.email.asViewField
        )
      }
  
      Ok(views.html.frontend.account_recover(
        maybeFormData.getOrElse(
          AccountRecoverFormView(email = Field(name = Fields.Email.name, values = List("")))
        )
      ))
    }
  }
}

object GetRecoverAccountEndpoint {

  def url() = {
      controllers.routes.WebsiteControllers.getRecoverAccount().url
//    WebsiteControllers.reverse(WebsiteControllers.getRecoverAccount)
  }
}