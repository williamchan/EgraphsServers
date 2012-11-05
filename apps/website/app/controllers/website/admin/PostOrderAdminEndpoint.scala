package controllers.website.admin

import models._
import enums.OrderReviewStatus
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import services.print.{PrintManufacturingInfo, LandscapeFramedPrint}

trait PostOrderAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters

  def postOrderAdmin(orderId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        orderStore.findById(orderId) match {
          case None => NotFound("No order with that id")
          case Some(order) => {
            val action = Form("action" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
            action match {
              case "approve" =>
              	order.approveByAdmin(admin).save()
              	Redirect(GetOrderAdminEndpoint.url(orderId))
              case "reject" =>
                val rejectionReason = Form(single("rejectionReason" -> text)).bindFromRequest.apply("rejectionReason").value
              	order.rejectByAdmin(admin, rejectionReason = rejectionReason).save()
              	Redirect(GetOrderAdminEndpoint.url(orderId))
              case "editMessages" => {
                val msgs = Form(tuple("recipientName" -> text, "messageToCelebrity" -> text, "requestedMessage" -> text)).bindFromRequest.value.get
                val recipientName = Option(msgs._1)
                val messageToCelebrity = Option(msgs._2)
                val requestedMessage = Option(msgs._3)
                val orderWithMsgs = order.copy(messageToCelebrity = messageToCelebrity, requestedMessage = requestedMessage).save()
                recipientName.map(name => orderWithMsgs.copy(recipientName = name).save())
                Redirect(GetOrderAdminEndpoint.url(orderId))
              }
              case "refund" => {
                order.refund()
                Redirect(GetOrderAdminEndpoint.url(orderId))
              }
              case "pending" => {
                order.withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
                Redirect(GetOrderAdminEndpoint.url(orderId))
              }
              // TODO(wchan): This should go away once there is exactly one PrintOrder record for every print order
              case "generateImages" => {
                egraphStore.findByOrder(orderId, egraphQueryFilters.publishedOrApproved).headOption match {
                  case None => Ok(<html><body>A published or approved egraph is required.</body></html>)
                  case Some(egraph) => {
                    val pngUrl = egraph.getSavedEgraphUrlAndImage(LandscapeFramedPrint.targetEgraphWidth)._1
                    val framedPrintImageUrl = egraph.getFramedPrintImageUrl
                    val csv = PrintManufacturingInfo.toCSVLine(buyerEmail = order.buyer.account.email,
                    	shippingAddress = "",
                    	partnerPhotoFile = egraph.framedPrintFilename)
                    Ok(views.html.Application.admin.admin_printinfo(framedPrintImageUrl, PrintManufacturingInfo.headerCSVLine, csv, Some(pngUrl)))
                  }
                }
              }
              case _ => Forbidden("Unsupported operation")
            }
          }
        }
      }
    }
  }
}
