package controllers.website.consumer

import models.CelebrityRequest
import models.CelebrityStore
import models.frontend.marketplace.RequestStarViewModel
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.Valid
import play.api.mvc.Action
import play.api.mvc.Controller
import services.AppConfig
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.EgraphsSession._
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import services.http.forms.FormConstraints
import services.http.{POSTControllerMethod, WithDBConnection}
import services.mvc.ImplicitHeaderAndFooterData
import services.request.PostCelebrityRequestHelper

private[controllers] trait PostRequestStarEndpoint extends ImplicitHeaderAndFooterData
  with PostCelebrityRequestHelper {
  this: Controller =>

  protected def celebrityStore: CelebrityStore
  protected def dbSession: DBSession
  protected def httpFilters: HttpFilters
  protected def postController: POSTControllerMethod

  /**
   * Store requested star and associated email for later notification.
   */
  def postRequestStar = postController(dbSettings = WithDBConnection(readOnly = true)) {
    Action { implicit request =>

      val form = PostRequestStarEndpoint.form

      form.bindFromRequest.fold(
        formWithErrors => BadRequest("Something has gone wrong with request a star form submit"),
        validForm => {
          val starName = validForm.starName

          val eitherCustomerAndAccountOrResult = httpFilters.requireCustomerLogin.filterInSession()
          eitherCustomerAndAccountOrResult match {

            case Right(customerAndAccount) => {
              val customerId = customerAndAccount._1.id

              completeRequestStar(starName, customerId)
              Redirect(controllers.routes.WebsiteControllers.getMarketplaceResultPage(vertical = ""))
            }
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
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RequestStarViewModel] = Form(
    mapping(
      "starName" -> text.verifying(nonEmpty)
    )(RequestStarViewModel.apply)(RequestStarViewModel.unapply))
}