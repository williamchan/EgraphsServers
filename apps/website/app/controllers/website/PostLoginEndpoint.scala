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

          val maybeRequestedStar = request.session.requestedStar
          maybeRequestedStar match {
            case None => {
              Redirect(getCustomerGalleryById(validForm.customerId)).withSession(
                request.session.withCustomerId(validForm.customerId).withHasSignedUp
              )
            }
            case Some(requestedStar) => {

              val customerId = validForm.customerId
              val maybeCelebrity = celebrityStore.findByPublicName(requestedStar)

              maybeCelebrity match {
                case None => play.Logger.info(requestedStar + " is not currently on Egraphs. Wah wah.")
                case Some(celebrity) => play.Logger.info("We already have that celebrity, silly! Buy an egraph from " + requestedStar + "!")
              }

              play.Logger.info("starName is " + requestedStar + ", customerId is " + customerId)
              Redirect(controllers.routes.WebsiteControllers.getMarketplaceResultPage(vertical = "")).withSession(
                request.session.withCustomerId(validForm.customerId).withHasSignedUp.removeRequestedStar)
            }
          }
        }
      }
    }
  }
}
