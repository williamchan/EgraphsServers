package controllers.website.admin

import models._
import enums.OrderReviewStatus
import enums.PrivacyStatus
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import services.print.{ PrintManufacturingInfo, LandscapeFramedPrint }
import play.api.templates.Html

trait PostOrderAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters
  protected def inventoryBatchStore: InventoryBatchStore
  protected def orderStore: OrderStore
  protected def printOrderStore: PrintOrderStore
  protected def productStore: ProductStore

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
                    Ok(views.html.Application.admin.admin_printinfo(
                      framedPrintImageUrl,
                      PrintManufacturingInfo.headerCSVLine,
                      csv,
                      Some(pngUrl),
                      Some(egraph.getStandaloneCertificateUrl)
                    ))
                  }
                }
              }
              case "changeRecipient" => {
                Form(single("newRecipientEmail" -> email)).bindFromRequest().fold(
                  errors => BadRequest(Html("<html><body>Invalid email. Double check the email you provided.</body></html>")),
                  newRecipientEmail => {
                    val maybeOk = for (
                      account <- accountStore.findByEmail(newRecipientEmail);
                      newRecipientId <- account.customerId
                    ) yield {
                      order.copy(recipientId=newRecipientId).save()
                      Redirect(GetOrderAdminEndpoint.url(orderId))
                    }

                    maybeOk.getOrElse(BadRequest(
                      Html("<html><body>Provided email " + newRecipientEmail + " doesn't correspond to a customer account.</body></html>")
                    ))
                  }
                )
              }
              case "changeBuyer" => {
                Form(single("newBuyerEmail" -> email)).bindFromRequest().fold(
                  errors => BadRequest(Html("<html><body>Invalid email. Double check the email you provided.</body></html>")),
                  newBuyerEmail => {
                    val maybeOk = for (
                      account <- accountStore.findByEmail(newBuyerEmail);
                      newBuyerId <- account.customerId
                    ) yield {
                      order.copy(buyerId=newBuyerId).save()
                      Redirect(GetOrderAdminEndpoint.url(orderId))
                    }
                    maybeOk.getOrElse(BadRequest(
                      Html("<html><body>Provided email " + newBuyerEmail + " doesn't correspond to a customer account.</body></html>")
                    ))
                  }
                )
              }
              case "changeProduct" => {
                Form(single("newProductId" -> number)).bindFromRequest().fold(
                  errors => BadRequest(Html("<html><body> Incorrect Product Id </body></html>")),
                  newProductId => {
                    val maybeOk = for{
                      product <- productStore.findById(newProductId)
                      inventoryBatch <- inventoryBatchStore.getActiveInventoryBatches(product).filter(batch => batch.hasInventory).headOption
                    } yield {
                      // create a new order
                      val newOrder = order.rejectAndCreateNewOrderWithNewProduct(product, inventoryBatch)
                      Redirect(GetOrderAdminEndpoint.url(newOrder.id))  
                    }
                    maybeOk.getOrElse(
                      BadRequest(Html("<html><body> Incorrect product id or no inventory batch for = " + newProductId + " </body></html>"))
                    )
                  }
                )
              }
              case "privacy" => {
                val privacyStatusString = Form("privacyStatus" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
                PrivacyStatus(privacyStatusString) match {
                  case Some(privacyEnum) => {
                    order.withPrivacyStatus(privacyEnum).save()
                    Redirect(GetOrderAdminEndpoint.url(order.id))
                  }
                  case None => BadRequest(Html("<html><body>Malformed privacy status</body></html>"))
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
