package controllers.website

import models._
import enums.PrivacyStatus
import play.api.data._
import play.api.libs.json.Json.toJson
import play.api.mvc.Controller
import play.api.mvc._
import play.api.data.Forms._
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import sjson.json.Serializer

private[controllers] trait PostOrderConfigureEndpoint { this: Controller =>
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore

  def postOrderPrivacy(orderId: Long) = postController() {
    httpFilters.requireCustomerLogin.inSession() { (customer, account) =>
      Action { implicit request =>
        val privacyStatusString = Form("privacyStatus" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
        val newPrivacyStatus = for (
          privacyStatus <- PrivacyStatus(privacyStatusString);
          order <- orderStore.findById(orderId.toLong);
          if order.recipient.id == customer.id
        ) yield {
          order.withPrivacyStatus(privacyStatus).save().privacyStatus
        }
        newPrivacyStatus match {
          case Some(privacy) => Ok(toJson(Map("privacyStatus" -> privacy.name)))
          case None => Ok(toJson(Map("error" -> true))) //TODO: PLAY20 should this be a BadRequest?
        }
      }
    }
  }  
}
