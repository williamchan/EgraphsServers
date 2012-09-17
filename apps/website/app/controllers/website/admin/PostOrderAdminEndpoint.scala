package controllers.website.admin

import models._
import enums.OrderReviewStatus
import play.mvc.results.Redirect
import play.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}
import services.http.SafePlayParams.Conversions._
import controllers.WebsiteControllers._
import controllers.WebsiteControllers

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
          val messageToCelebrity = params.getOption("messageToCelebrity")
          val requestedMessage = params.getOption("requestedMessage")
          order.copy(messageToCelebrity = messageToCelebrity, requestedMessage = requestedMessage).save()
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
        case "generateImages" => {
              import services.http.SafePlayParams.Conversions._
              val errorOrBlobUrl = for (
                orderId <- params.getLongOption("orderId").toRight("orderId param required").right;
                fulfilledOrder <- orderStore
                                    .findFulfilledWithId(orderId)
                                    .toRight("No fulfilled order with ID" + orderId + "found")
                                    .right
              ) yield {
                val FulfilledOrder(order, egraph) = fulfilledOrder
                val product = order.product
                val productPhoto = product.photoImage
                val targetWidth = {
                  val masterWidth = productPhoto.getWidth
                  if (masterWidth < PrintOrder.defaultPngWidth) masterWidth else PrintOrder.defaultPngWidth
                }

                egraph.image(productPhoto)
                  .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
                  .scaledToWidth(targetWidth)
                  .rasterized
                  .saveAndGetUrl(services.blobs.AccessPolicy.Public)
              }

              errorOrBlobUrl.fold(error => error, url => url)
        }
        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}
