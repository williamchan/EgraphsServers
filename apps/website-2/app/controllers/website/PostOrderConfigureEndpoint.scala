package controllers.website

import services.http.POSTControllerMethod
import models._
import enums.PrivacyStatus
import play.api.mvc.Controller
import services.http.SafePlayParams.Conversions._
import sjson.json.Serializer
import play.api.mvc._
import play.api.libs.json.Json.toJson
import services.Utils
import services.http.filters.HttpFilters

private[controllers] trait PostOrderConfigureEndpoint { this: Controller =>
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore

  def postOrderPrivacy(orderId: Long) = postController() {
    httpFilters.requireCustomerLogin.inSession() { (customer, account) =>
      Action { implicit request =>
        val params = request.queryString
        val privacyStatusOption = Utils.getOptionFirstInSeq(params.get("privacyStatus"))

        println("orderId " + orderId)
        println("privacyStatusOption" + privacyStatusOption)
        val newPrivacyStatus = for (
          privacyStatusString <- privacyStatusOption;
          privacyStatus <- PrivacyStatus(privacyStatusString);
          order <- orderStore.findById(orderId.toLong);
          if order.recipient.id == customer.id
        ) yield {
          order.withPrivacyStatus(privacyStatus).save().privacyStatus
        }
  
        newPrivacyStatus match {
          case Some(privacy) => Ok(toJson(Map("privacyStatus" -> "privacy.name")))
          case None => Ok(toJson(Map("error" -> true))) //TODO: PLAY20 should this be a BadRequest?
        }
      }
    }
  }  
}
