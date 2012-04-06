package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{ControllerMethod, AdminRequestFilters}
import models.{ProductStore, Celebrity}

private[controllers] trait GetUpdateCelebrityProductEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def productStore: ProductStore

  /**
   * Serves up the HTML for the Create Celebrity page.
   */
  def getUpdateCelebrityProduct(productId: Long) = controllerMethod() {
    adminFilters.requireCelebrity { (celebrity) =>
      val product = productStore.findById(productId).get
      flash.put("productId", product.id)
      flash.put("productName", product.name)
      flash.put("productDescription", product.description)
      flash.put("storyTitle", product.storyTitle)
      flash.put("storyText", product.storyText)

      GetProductDetail.getCelebrityProductDetail(celebrity = celebrity, isCreate = false)
    }
  }
}

object GetUpdateCelebrityProductEndpoint {

  def url(celebrity: Celebrity, productId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getUpdateCelebrityProduct", Map("celebrityId" -> celebrity.id.toString, "productId" -> productId.toString))
  }
}