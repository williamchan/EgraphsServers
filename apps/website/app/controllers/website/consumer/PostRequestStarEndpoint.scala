package controllers.website.consumer

import models.CelebrityRequest
import models.frontend.marketplace.RequestStarViewModel
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.Valid
import play.api.mvc.Action
import play.api.mvc.Controller
import services.AppConfig
import services.http.EgraphsSession._
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import services.http.forms.FormConstraints
import services.http.{POSTControllerMethod, WithDBConnection}
import services.mvc.ImplicitHeaderAndFooterData
import services.request.PostCelebrityRequestHelper
import controllers.routes

private[controllers] trait PostRequestStarEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def httpFilters: HttpFilters
  protected def postController: POSTControllerMethod

  /**
   * Store requested star and associated email for later notification.
   */
  def postRequestStar = postController() {
    Action { implicit request =>

      val form = PostRequestStarEndpoint.form

      form.bindFromRequest.fold(
        formWithErrors => BadRequest("Something has gone wrong with request a star form submit"),
        validForm => {
          val starName = validForm.starName

          val eitherCustomerAndAccountOrResult = httpFilters.requireCustomerLogin.filterInSession()
          eitherCustomerAndAccountOrResult match {

            // Logged in case
            case Right((customer, account)) => {
              val customerId = customer.id

              PostCelebrityRequestHelper.completeRequestStar(starName, customerId, account.email)

              request.session.afterLoginRedirectUrl match {
                case Some(afterLoginRedirectUrl) => Redirect(afterLoginRedirectUrl)
                case None => Redirect(controllers.routes.WebsiteControllers.getMarketplaceResultPage(""))
              }
            }

            // Not logged in case
            case Left(result) => {
              // redirect to login page, add requested star name to session for post-login lookup
              Redirect(controllers.routes.WebsiteControllers.getLogin(
                maybeBannerMessage = Some("**Please log in to complete your request**")
              )).withSession(request.session.withRequestedStar(starName))
            }
          }
        }
      )
    }
  }
}

object PostRequestStarEndpoint {
  def formName = "request-star-form"
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RequestStarViewModel] = Form(
    mapping(
      "starName" -> text.verifying(nonEmpty)
    )(RequestStarViewModel.apply)(RequestStarViewModel.unapply))
}