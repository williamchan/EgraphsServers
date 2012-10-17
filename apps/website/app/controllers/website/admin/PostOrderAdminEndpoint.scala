package controllers.website.admin

import models._
import enums.OrderReviewStatus
import play.mvc.results.Redirect
import play.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}
import services.http.SafePlayParams.Conversions._
import controllers.WebsiteControllers
import controllers.WebsiteControllers._
import services.print.{PrintManufacturingInfo, LandscapeFramedPrint}

trait PostOrderAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def orderStore: OrderStore
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters

  def postOrderAdmin(orderId: Long) = postController() {
    adminFilters.requireOrder {(order, admin) =>
      params.get("action") match {
        case "approve" =>
          order.approveByAdmin(admin).save()
          new Redirect(GetOrderAdminEndpoint.url(orderId).url)
        case "reject" =>
          val rejectionReason = params.get("rejectionReason")
          order.rejectByAdmin(admin, rejectionReason = Some(rejectionReason)).save()
          new Redirect(WebsiteControllers.reverse(getOrderAdmin(orderId)).url)
        case "editMessages" => {
          val recipientName = params.getOption("recipientName")
          val messageToCelebrity = params.getOption("messageToCelebrity")
          val requestedMessage = params.getOption("requestedMessage")
          val orderWithMsgs = order.copy(messageToCelebrity = messageToCelebrity, requestedMessage = requestedMessage).save()
          recipientName.map(name => orderWithMsgs.copy(recipientName = name).save())
          new Redirect(WebsiteControllers.reverse(getOrderAdmin(orderId)).url)
        }
        case "refund" => {
          order.refund()
          new Redirect(WebsiteControllers.reverse(getOrderAdmin(orderId)).url)
        }
        case "pending" => {
          order.withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
          new Redirect(WebsiteControllers.reverse(getOrderAdmin(orderId)).url)
        }
        // TODO(wchan): This should go away once there is exactly one PrintOrder record for every print order
        case "generateImages" => {
          egraphStore.findByOrder(orderId, egraphQueryFilters.publishedOrApproved).headOption match {
            case None => <html><body>A published or approved egraph is required.</body></html>
            case Some(egraph) => {
              val pngUrl = egraph.getSavedEgraphUrlAndImage(LandscapeFramedPrint.targetEgraphWidth)._1
              val framedPrintImageUrl = egraph.getFramedPrintImageUrl
              val csv = PrintManufacturingInfo.toCSVLine(buyerEmail = order.buyer.account.email,
                shippingAddress = "",
                partnerPhotoFile = egraph.framedPrintFilename)
              <html>
                <body>
                  <a href={pngUrl} target="_blank">{pngUrl}</a>
                  <br/>
                  <a href={framedPrintImageUrl} target="_blank">{framedPrintImageUrl}</a>
                  <br/>
                  {PrintManufacturingInfo.headerCSVLine}
                  <br/>
                  {csv}
                </body>
              </html>
            }
          }
        }
        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}
