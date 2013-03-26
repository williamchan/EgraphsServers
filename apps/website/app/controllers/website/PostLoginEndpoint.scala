package controllers.website

import play.api.mvc._
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import controllers.routes.WebsiteControllers.getLogin
import services.http.POSTControllerMethod
import services.http.forms.CustomerLoginFormFactory
import services.http.EgraphsSession
import services.http.EgraphsSession.Key._
import services.http.EgraphsSession.Conversions._
import models._

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import services.http.forms.Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def customerLoginForms: CustomerLoginFormFactory

  def postLogin() = postController() {
    Action { implicit request =>
      // Read a CustomerLoginForm from the params
      val nonValidatedForm = customerLoginForms(request.asFormReadable)
  
      // Handle valid or error cases
      nonValidatedForm.errorsOrValidatedForm match {
        case Left(errors) =>
          nonValidatedForm.redirectThroughFlash(getLogin().url)(request.flash)
  
        case Right(validForm) => {

          // Find out whether the user is logging in to complete their celebrity request
          val redirectCall: Call = request.session.requestedStarRedirectOrCall(
            validForm.customerId,
            validForm.email,
            controllers.routes.WebsiteControllers.getCustomerGalleryById(validForm.customerId))

          Redirect(redirectCall).withSession(
            request.session
              .withCustomerId(validForm.customerId)
              .removeRequestedStar
              .removeAfterLoginRedirectUrl
          ).withCookies(Cookie(HasSignedUp.name, true.toString, maxAge = Some(EgraphsSession.COOKIE_MAX_AGE)))
        }
      }
    }
  }
}
