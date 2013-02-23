package controllers.website.consumer

import egraphs.authtoken.AuthenticityToken
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
import services.http.EgraphsSession._
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import services.http.forms.FormConstraints
import services.http.{POSTControllerMethod, WithDBConnection}
import services.mail.BulkMailList
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait PostRequestStarEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def celebrityStore: CelebrityStore
  protected def httpFilters: HttpFilters
  protected def postController: POSTControllerMethod

  /**
   * Store requested star and associated email for later notification.
   */
  def postRequestStar = postController(dbSettings = WithDBConnection(readOnly = true)) {
    Action { implicit request =>

      val eitherCustomerAndAccountOrResult = httpFilters.requireCustomerLogin.filterInSession()
      eitherCustomerAndAccountOrResult match {
        case Right(customerAndAccount) => {
          Ok("Yay! We're logged in and the customer ID is " + customerAndAccount._1.id)
        }
        case Left(result) => {
          Redirect("/login")
        }
      }

//      val form = PostRequestStarEndpoint.form
//
//      form.bindFromRequest.fold(
//        formWithErrors => {
//          BadRequest("Something's gone wrong with request a star form submit")
//        },
//        validForm => {
//
//          val starName = validForm.starName
//          val maybeCelebrity = celebrityStore.findByPublicName(starName)
//
//          maybeCelebrity match {
//            case None => play.Logger.info(starName + " is not currently on Egraphs. Wah wah.")
//            case Some(celebrity) => play.Logger.info("We already have that celebrity, silly! Buy an egraph from " + starName + "!")
//          }
//
//          play.Logger.info("starName is " + validForm.starName + ", customerId is " + validForm.customerId)
//          Redirect(controllers.routes.WebsiteControllers.getMarketplaceResultPage(vertical = ""))
//        }
//      )
    }
  }
}

object PostRequestStarEndpoint {
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RequestStarViewModel] = Form(
    mapping(
      "starName" -> text.verifying(nonEmpty)
      //      "customerId" -> longNumber
      //      "requesterName" -> text.verifying(nonEmpty),
      //      "email" -> email.verifying(nonEmpty)
      )(RequestStarViewModel.apply)(RequestStarViewModel.unapply))
}