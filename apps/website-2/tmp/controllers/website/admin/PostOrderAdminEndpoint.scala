package controllers.website.admin

import models._
import enums.OrderReviewStatus
import services.http.{POSTControllerMethod, AdminRequestFilters}
import services.http.SafePlayParams.Conversions._
import controllers.WebsiteControllers
import controllers.WebsiteControllers._
import services.print.{PrintManufacturingInfo, LandscapeFramedPrint}

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
          val errorOrBlobUrl = for (
            orderId <- params.getLongOption("orderId").toRight("orderId param required").right;
            fulfilledOrder <- orderStore
              .findFulfilledWithId(orderId)
              .toRight("No fulfilled order with ID" + orderId + "found")
              .right
          ) yield {
            val FulfilledOrder(order, egraph) = fulfilledOrder
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

          errorOrBlobUrl.fold(error => error, url => url)
        }
        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}
