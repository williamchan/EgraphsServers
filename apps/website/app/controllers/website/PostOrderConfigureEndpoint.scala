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
import play.api.mvc.Results.{BadRequest, NotFound, Forbidden}

private[controllers] trait PostOrderConfigureEndpoint { this: Controller =>
  import PostOrderConfigureEndpoint.{errorMalformed, errorOrderNotFound, errorNotOwner}
  
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore

  def postOrderPrivacy(orderId: Long) = postController() {
    httpFilters.requireCustomerLogin.inSession() { (customer, account) =>
      Action { implicit request =>
        val privacyStatusString = Form("privacyStatus" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
        val httpResults = for (
          privacyStatus <- PrivacyStatus(privacyStatusString).toRight(left=errorMalformed).right;
          order <- orderStore.findById(orderId.toLong).toRight(left=errorOrderNotFound).right;
          _ <- forbiddenOrOwnsOrder(customer, order).right
        ) yield {
          order.withPrivacyStatus(privacyStatus).save().privacyStatus
          Ok(toJson(Map("privacyStatus" -> privacyStatus.name)))
        }
        
        httpResults.merge
      }
    }
  }
  
  //
  // Private members
  //
  private def forbiddenOrOwnsOrder(customer: Customer, order: Order): Either[Result, Unit] = {
    if (order.recipient.id == customer.id) Right() else Left(errorNotOwner)
  }
}


private[controllers] object PostOrderConfigureEndpoint {
  val errorMalformed = BadRequest("Malformed privacy status")
  val errorOrderNotFound = NotFound("Egraph not found")
  val errorNotOwner = Forbidden("Only the egraph owner may alter its settings.")
}