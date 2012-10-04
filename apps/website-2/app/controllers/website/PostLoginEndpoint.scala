package controllers.website

import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import models._
import services.http.POSTControllerMethod
import services.http.forms.{CustomerLoginFormFactory, Form}
import play.api.mvc.Action
import controllers.routes.WebsiteControllers.{getLogin, getCustomerGalleryById}

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerLoginForms: CustomerLoginFormFactory

  def postLogin() = postController() {
    Action { implicit request =>
      // Read a CustomerLoginForm from the params
      val params = request.queryString
      val nonValidatedForm = customerLoginForms(request.asFormReadable)
  
      // Handle valid or error cases
      nonValidatedForm.errorsOrValidatedForm match {
        case Left(errors) =>
          nonValidatedForm.redirectThroughFlash(getLogin().url)(request.flash)
  
        case Right(validForm) =>          
          Redirect(getCustomerGalleryById(validForm.customerId)).withSession(
            request.session + (WebsiteControllers.customerIdKey -> validForm.customerId.toString)
          )
      }
    }
  }
}
