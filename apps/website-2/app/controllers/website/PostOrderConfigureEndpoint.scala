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

  def postOrderPrivacy() = postController() {     
    httpFilters.requireCustomerLogin.inSession() { (customer, account) =>
      Action { request =>
        val params = request.queryString
  
        val orderIdOption = Utils.getOptionFirstInSeq(params.get("orderId"))
        val privacyStatusOption = Utils.getOptionFirstInSeq(params.get("privacyStatus"))

        println("orderIdOption " + orderIdOption)
        println("privacyStatusOption" + privacyStatusOption)
        val newPrivacyStatus = for (
          privacyStatusString <- privacyStatusOption;
          privacyStatus <- PrivacyStatus(privacyStatusString);
          orderId <- orderIdOption;
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
