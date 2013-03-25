package controllers.website

import egraphs.playutils.FlashableForm._
import play.api.mvc._
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import controllers.routes.WebsiteControllers.getLogin
import services.http.POSTControllerMethod
import services.http.EgraphsSession
import services.http.EgraphsSession.Key._
import services.http.EgraphsSession.Conversions._
import services.login.PostLoginHelper
import models._

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import services.http.forms.Form.Conversions._

  protected def accountStore: AccountStore
  protected def postController: POSTControllerMethod

  def postLogin() = postController() {
    Action { implicit request =>

      val (form, formName) = (PostLoginHelper.form, PostLoginHelper.formName)

      form.bindFromRequest.fold(
        formWithErrors => Redirect(controllers.routes.WebsiteControllers.getLogin()).flashingFormData(formWithErrors, formName),
        validForm => {

          val email = validForm.loginEmail
          val customerAccount = accountStore.findByEmail(email).getOrElse(
            throw new RuntimeException("The email provided by existing user " +
              email + " somehow passed validation but failed while attempting to retrieve the account"))

          val customerId = customerAccount.customerId.getOrElse(
            throw new RuntimeException("The account associated with existing user " +
              email + " somehow passed validation but failed while attempting to get the customer id"))

          // Find out whether the user is logging in to complete their celebrity request
          val redirectCall: Call = request.session.requestedStarRedirectOrCall(
            customerId,
            email,
            controllers.routes.WebsiteControllers.getCustomerGalleryById(customerId))

          Redirect(redirectCall).withSession(
            request.session
              .withCustomerId(validForm.customerId)
              .removeRequestedStar
              .removeAfterLoginRedirectUrl
          ).withCookies(Cookie(HasSignedUp.name, true.toString, maxAge = Some(EgraphsSession.COOKIE_MAX_AGE)))
        }
      )
    }
  }
}
