package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import play.api.mvc.AnyContent
import play.api.mvc.Request
import controllers.WebsiteControllers
import controllers.routes.WebsiteControllers.{getLogin, getCustomerGalleryById}
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.POSTControllerMethod
import services.http.forms.CustomerLoginFormFactory
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._
import services.request.PostCelebrityRequestHelper
import models._

private[controllers] trait PostLoginEndpoint extends PostCelebrityRequestHelper { this: Controller =>
  import services.http.forms.Form.Conversions._

  protected def celebrityStore: CelebrityStore
  protected def dbSession: DBSession
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
          maybeRequestedStar match {
            case None => {
              Redirect(getCustomerGalleryById(validForm.customerId)).withSession(
                request.session.withCustomerId(validForm.customerId).withHasSignedUp
              )
            }
            case Some(requestedStar) => completeRequestStar(requestedStar, validForm.customerId)(request)
          }
        }
      }
    }
  }
}