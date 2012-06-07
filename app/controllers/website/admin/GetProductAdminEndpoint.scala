package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{ControllerMethod, AdminRequestFilters}
import models.{ProductStore, Celebrity}
import controllers.website.GetCelebrityProductEndpoint
import services.payment.Payment

private[controllers] trait GetProductAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def productStore: ProductStore
  protected def payment: Payment

  def getProductAdmin(productId: Long, action: Option[String] = None) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val productOption = productStore.findById(productId)
      val product = productOption.get

      action match {
        case Some("preview") => {
          GetCelebrityProductEndpoint.html(celebrity = product.celebrity, product = product, payment = payment)
        }

        case _ => {
          flash.put("productId", product.id)
          flash.put("productName", product.name)
          flash.put("productDescription", product.description)
          flash.put("signingOriginX", product.signingOriginX)
          flash.put("signingOriginY", product.signingOriginY)
          flash.put("storyTitle", product.storyTitle)
          flash.put("storyText", product.storyText)
          flash.put("publishedStatusString", product.publishedStatus.toString)

          GetProductDetail.getCelebrityProductDetail(celebrity = product.celebrity, isCreate = false, product = productOption)
        }
      }
    }
  }
}

object GetProductAdminEndpoint {

  def url(productId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getProductAdmin", Map("productId" -> productId.toString))
  }
}