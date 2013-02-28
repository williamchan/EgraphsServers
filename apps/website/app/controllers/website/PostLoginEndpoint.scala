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
import services.request.PostCelebrityRequestHelper
import models._

private[controllers] trait PostLoginEndpoint extends PostCelebrityRequestHelper { this: Controller =>
  import services.http.forms.Form.Conversions._

  protected def celebrityStore: CelebrityStore
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
          nonValidatedForm.redirectThroughFlash(getLogin(None).url)(request.flash)
  
        case Right(validForm) => {

          // Find out whether the user is logging in to complete their celebrity request
          val maybeRequestedStar = request.session.requestedStar
          val redirectCall: Call = maybeRequestedStar match {
            case None => controllers.routes.WebsiteControllers.getCustomerGalleryById(validForm.customerId)
            case Some(requestedStar) => {
              completeRequestStar(requestedStar, validForm.customerId)
              controllers.routes.WebsiteControllers.getMarketplaceResultPage(vertical = "")
            }
          }
          Redirect(redirectCall).withSession(
            request.session.withCustomerId(validForm.customerId).removeRequestedStar
          ).withCookies(Cookie(HasSignedUp.name, true.toString, maxAge = Some(EgraphsSession.COOKIE_MAX_AGE)))
        }
      }
    }
  }
}
