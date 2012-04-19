package controllers.website.admin

import models._
import play.mvc.results.Redirect
import play.mvc.Controller
import services.http.{SecurityRequestFilters, AdminRequestFilters, ControllerMethod}

trait PostOrderAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def adminFilters: AdminRequestFilters
  protected def orderStore: OrderStore

  def postOrderAdmin(orderId: Long) = controllerMethod() {

    securityFilters.checkAuthenticity{
      adminFilters.requireOrder {(order, admin) =>
        val reviewStatusParam = params.get("reviewStatus")
        Order.ReviewStatus.all.get(reviewStatusParam) match {
          case None => Forbidden("Not a valid review status")
          case Some(Order.ReviewStatus.ApprovedByAdmin) => {
            order.approveByAdmin(admin).save()
            new Redirect(GetOrderAdminEndpoint.url(orderId).url)
          }
          case Some(Order.ReviewStatus.RejectedByAdmin) => {
            val rejectionReason = params.get("rejectionReason")
            order.rejectByAdmin(admin, rejectionReason = Some(rejectionReason)).save()
            new Redirect(GetOrderAdminEndpoint.url(orderId).url)
          }
          case _ => Forbidden("Unsupported operation")
        }
      }
    }
  }
}
