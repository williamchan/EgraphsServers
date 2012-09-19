package controllers.website

import services.http.{CustomerRequestFilters, POSTControllerMethod}
import models._
import enums.PrivacyStatus
import play.api.mvc.Controller
import play.mvc.results.RenderJson
import services.http.SafePlayParams.Conversions._
import sjson.json.Serializer
import play.api.mvc._
import services.Utils

private[controllers] trait PostOrderConfigureEndpoint { this: Controller =>
  protected def postController: POSTControllerMethod
  protected def customerFilters: CustomerRequestFilters
  protected def orderStore: OrderStore

  def postOrderPrivacy() = Action { implicit request =>
    postController() {
      customerFilters.requireCustomerLogin { (customer, account) =>
        {
        val params = request.queryString
  
        val orderIdOption = Utils.getOptionFirstInSeq(params.get("orderId"))
        val privacyStatusOption = Utils.getOptionFirstInSeq(params.get("privacyStatus"))

        println("orderIdOption " + orderIdOption)
        println("privacyStatusOption" + privacyStatusOption)
        val newPrivacyStatus =
          for(privacyStatusString <- privacyStatusOption;
            privacyStatus <- PrivacyStatus(privacyStatusString);
            orderId <- orderIdOption;
            order <- orderStore.findById(orderId.toLong);
            if order.recipient.id == customer.id)
          yield{
            order.withPrivacyStatus(privacyStatus).save().privacyStatus
          }
  
          newPrivacyStatus match {
            case Some(privacy) => new RenderJson(Serializer.SJSON.toJSON(Map("privacyStatus" -> privacy.name)))
            case None => new RenderJson(Serializer.SJSON.toJSON(Map("error" -> true)))
          }
  
        }
      }
    }
  }
}
