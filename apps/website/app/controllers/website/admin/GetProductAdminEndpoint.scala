package controllers.website.admin

import play.api.mvc.Controller
import models.ProductStore
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetProductAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def productStore: ProductStore

  def getProductAdmin(productId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      httpFilters.requireProductId(productId) { product =>
        Action { implicit request =>
          // TODO: Would be nice to have a preview mode for this.
          implicit val flash = request.flash + 
          	("productId" -> product.id.toString) + 
          	("productName" -> product.name) + 
          	("productDescription" -> product.description) + 
          	("priceInCurrency" -> ("%.2f" format product.priceInCurrency)) + 
          	("signingOriginX" -> product.signingOriginX.toString) + 
          	("signingOriginY" -> product.signingOriginY.toString) + 
          	("storyTitle" -> product.storyTitle) + 
          	("storyText" -> product.storyText) + 
          	("publishedStatusString" -> product.publishedStatus.toString)
          GetProductDetail.getCelebrityProductDetail(celebrity = product.celebrity, product = Option(product))
      	}
      }
    }
  }
}

object GetProductAdminEndpoint {

  def url(productId: Long) = {
    controllers.routes.WebsiteControllers.getProductAdmin(productId).url
  }
}