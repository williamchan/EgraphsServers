package controllers.website.admin

import models._
import play.mvc.results.Redirect
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}
import services.http.SafePlayParams.Conversions._

trait PostOrderAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def orderStore: OrderStore

  def postOrderAdmin(orderId: Long) = postController() {
    adminFilters.requireOrder {(order, admin) =>
      params.get("action") match {
        case "approve" =>
          order.approveByAdmin(admin).save()
          new Redirect(GetOrderAdminEndpoint.url(orderId).url)
        case "reject" =>
          val rejectionReason = params.get("rejectionReason")
          order.rejectByAdmin(admin, rejectionReason = Some(rejectionReason)).save()
          new Redirect(GetOrderAdminEndpoint.url(orderId).url)
        case "editMessages" => {
          val messageToCelebrity = params.getOption("messageToCelebrity")
          val requestedMessage = params.getOption("requestedMessage")
          order.copy(messageToCelebrity = messageToCelebrity, requestedMessage = requestedMessage).save()
          new Redirect(GetOrderAdminEndpoint.url(orderId).url)
        }

        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}
