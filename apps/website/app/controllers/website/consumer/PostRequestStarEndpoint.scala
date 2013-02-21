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
    //httpFilters.requireCustomerLogin.inSession() { case (customer, account) => // do we want to require customer login?!
      Action { implicit request =>
        
        Ok("you're in the controller!")
      
//      val requestForm = Form(
//        mapping(
//          "starName" -> text.verifying(nonEmpty),
//          "email" -> email.verifying(nonEmpty)
//        )(PostRequestStarForm.apply)(PostRequestStarForm.unapply)
//      )
//      
//      requestForm.bindFromRequest.fold(
//        formWithErrors => {
//          BadRequest("We're gonna need a valid email address")
//        },
//        validForm => {
//          //bulkMailList.subscribeNewAsync(validForm)
//          val starName = validForm.starName
//          
//          val maybeCelebrity = celebrityStore.findByPublicName(starName)
//          
//          maybeCelebrity match {
//            case None => play.Logger.info(starName + " is not currently on Egraphs. Wah wah.")
//            case Some(celebrity) => play.Logger.info("We already have that celebrity, silly! Buy an egraph from " + starName + "!")
//          }
//          
//          play.Logger.info("email is " + validForm.email + ", starName is " + validForm.starName)
//          Ok("star requested").withSession(request.session.withHasSignedUp)
//        }
//      )
    }
  }
  //}
}

object PostRequestStarEndpoint {
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RequestStarViewModel] = Form(
    mapping(
      "starName" -> text.verifying(nonEmpty),
      //"requesterName" -> text.verifying(nonEmpty),
      "email" -> email.verifying(nonEmpty)
    )(RequestStarViewModel.apply)(RequestStarViewModel.unapply)
  )
}