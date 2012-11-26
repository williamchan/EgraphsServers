package controllers.website.admin

import models._
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid

trait PostFreegraphAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def customerStore: CustomerStore
  
  case class PostFreegraphForm(
      recipientName: String, 
      recipientEmail: String, 
      buyerId: Long, 
      productId: Long, 
      inventoryBatchId: Long, 
      messageToCelebrity: String, 
      requestedMessage: String)
  
  /**
   * For updating an existing Account.
   */
  def postFreegraphAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val freegraphForm = Form(
          mapping(
            "recipientName" -> text.verifying(nonEmpty),
            "recipientEmail" -> email.verifying(nonEmpty),
            "buyerId" -> longNumber,
            "productId" -> longNumber,
            "inventoryBatchId" -> longNumber,
            "messageToCelebrity" -> text.verifying(nonEmpty),
            "requestedMessage" -> text.verifying(nonEmpty)
          )(PostFreegraphForm.apply)(PostFreegraphForm.unapply))
        
        freegraphForm.bindFromRequest.fold(
          formWithErrors => {
            Redirect(controllers.routes.WebsiteControllers.getCreateFreegraphAdmin).flashing("errors" -> formWithErrors.errors.head.message.toString())
          },
          validForm => {
            
            val customer = customerStore.findOrCreateByEmail(email = validForm.recipientEmail, name = validForm.recipientName)
            val order = Order(
              recipientId = customer.id,
              recipientName = validForm.recipientName,
              buyerId= validForm.buyerId,
              productId = validForm.productId,
              inventoryBatchId = validForm.inventoryBatchId,
              messageToCelebrity = Some(validForm.messageToCelebrity),
              requestedMessage = Some(validForm.requestedMessage)
            ).save()
            Redirect(controllers.routes.WebsiteControllers.getOrderAdmin(order.id))
          }
        )
      }
    }
  }
}