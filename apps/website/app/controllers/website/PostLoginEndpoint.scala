package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import controllers.routes.WebsiteControllers.{getLogin, getCustomerGalleryById}
import services.http.POSTControllerMethod
import services.http.forms.CustomerLoginFormFactory
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._
import models._

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import services.http.forms.Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerLoginForms: CustomerLoginFormFactory

  def postLogin() = postController() {
    Action { implicit request =>
      // Read a CustomerLoginForm from the params
      val nonValidatedForm = customerLoginForms(request.asFormReadable)
  
      // Handle valid or error cases
      nonValidatedForm.errorsOrValidatedForm match {
        case Left(errors) =>
          nonValidatedForm.redirectThroughFlash(getLogin().url)(request.flash)
  
        case Right(validForm) =>          
          Redirect(getCustomerGalleryById(validForm.customerId)).withSession(
            request.session.withCustomerId(validForm.customerId).withHasSignedUp
          )
      }
    }
  }
}
