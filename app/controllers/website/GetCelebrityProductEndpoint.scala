package controllers.website

import play.mvc.Controller
import services.Utils

import models._
import services.payment.Payment
import play.mvc.Router.ActionDefinition
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}

private[controllers] trait GetCelebrityProductEndpoint { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def payment: Payment

  /**
   * Serves up the HTML of a single Celebrity Product page.
   */
  def getCelebrityProduct = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celebrity, product) =>
      // Get errors and param values from previous unsuccessful buy
      val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
      val fieldDefaults = (paramName: String) => paramName match {
        case "cardNumber" => "4242424242424242"
        case "cardCvc" => "333"
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }

      // Render the page
      views.Application.html.product(celebrity, product, errorFields, fieldDefaults, payment)
    }
  }
}

object GetCelebrityProductEndpoint {
  /**
   * Reverses the product index url for a particular celebrity and product
   */
  def url(celebrity:Celebrity, product:Product): ActionDefinition = {
    urlFromSlugs(celebrity.urlSlug.get, product.urlSlug)
  }
  
  def urlFromSlugs(celebrityUrlSlug: String, productUrlSlug: String): ActionDefinition = {
    val params: Map[String, AnyRef] = Map(
      "celebrityUrlSlug" -> celebrityUrlSlug,
      "productUrlSlug" -> productUrlSlug
    )

    Utils.lookupUrl("WebsiteControllers.getCelebrityProduct", params)    
  }
}





