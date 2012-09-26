package controllers.website

import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import models._
import services.http.POSTControllerMethod
import services.http.forms.{CustomerLoginFormFactory, Form}
import play.api.mvc.Action

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerLoginForms: CustomerLoginFormFactory

  def postLogin() = postController() {
    Action { request =>
      // Read a CustomerLoginForm from the params
      val params = request.queryString
      val nonValidatedForm = customerLoginForms(params.asFormReadable)
  
      // Handle valid or error cases
      nonValidatedForm.errorsOrValidatedForm match {
        case Left(errors) =>
          nonValidatedForm.redirectThroughFlash(GetLoginEndpoint.url().url)
  
        case Right(validForm) =>
          session.put(WebsiteControllers.customerIdKey, validForm.customerId)
          new Redirect(reverse(WebsiteControllers.getCustomerGalleryById(validForm.customerId)).url)
      }
    }
  }
}
