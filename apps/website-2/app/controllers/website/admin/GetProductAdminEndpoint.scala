package controllers.website.admin

import play.mvc.Controller
import services.http.{ControllerMethod, AdminRequestFilters}
import models.ProductStore
import controllers.WebsiteControllers

private[controllers] trait GetProductAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def productStore: ProductStore

  def getProductAdmin(productId: Long, action: Option[String] = None) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val product = productStore.get(productId)

      action match {
//        case Some("preview") => {
//          GetCelebrityProductEndpoint.html(celebrity = product.celebrity, product = product, payment = payment)
//        }

        case _ => {
          flash.put("productId", product.id)
          flash.put("productName", product.name)
          flash.put("productDescription", product.description)
          flash.put("priceInCurrency", ("%.2f" format product.priceInCurrency))
          flash.put("signingOriginX", product.signingOriginX)
          flash.put("signingOriginY", product.signingOriginY)
          flash.put("storyTitle", product.storyTitle)
          flash.put("storyText", product.storyText)
          flash.put("publishedStatusString", product.publishedStatus.toString)

          GetProductDetail.getCelebrityProductDetail(celebrity = product.celebrity, isCreate = false, product = Option(product))
        }
      }
    }
  }
}

object GetProductAdminEndpoint {

  def url(productId: Long) = {
    WebsiteControllers.reverse(WebsiteControllers.getProductAdmin(productId))
  }
}