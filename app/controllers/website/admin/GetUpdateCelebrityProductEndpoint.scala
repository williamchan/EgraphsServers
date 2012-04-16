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
    val productOption = productStore.findById(productId)
    val product = productOption.get
    flash.put("productId", product.id)
    flash.put("productName", product.name)
    flash.put("productDescription", product.description)
    flash.put("storyTitle", product.storyTitle)
    flash.put("storyText", product.storyText)

    GetProductDetail.getCelebrityProductDetail(celebrity = product.celebrity, isCreate = false, product = productOption)
  }
}

object GetUpdateCelebrityProductEndpoint {

  def url(productId: Long, celebrity: Celebrity) = {
    Utils.lookupUrl("WebsiteControllers.getUpdateCelebrityProduct", Map("celebrityId" -> celebrity.id.toString, "productId" -> productId.toString))
  }
}