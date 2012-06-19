package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import models._
import services.http.POSTControllerMethod
import services.http.forms.{CustomerLoginFormFactory, Form}

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerLoginForms: CustomerLoginFormFactory

  def postLogin() = postController() {
    // Read a CustomerLoginForm from the params
    val nonValidatedForm = customerLoginForms(params.asFormReadable)

    // Handle valid or error cases
    nonValidatedForm.errorsOrValidatedForm match {
      case Left(errors) =>
        nonValidatedForm.redirectThroughFlash(GetLoginEndpoint.url().url)

      case Right(validForm) =>
        session.put(WebsiteControllers.customerIdKey, validForm.customerId)
        new Redirect(Utils.lookupUrl("WebsiteControllers.getRootEndpoint").url)
    }
  }
}
