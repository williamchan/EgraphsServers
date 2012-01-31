package controllers.browser

import play.mvc.Controller
import services.Utils

import models._
import services.http.CelebrityAccountRequestFilters

/**
 * Serves pages relating to a particular product of a celebrity.
 */
private[controllers] trait GetCelebrityProductEndpoint { this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrityProduct = {
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
      views.Application.html.product(celebrity, product, errorFields, fieldDefaults)
    }
  }
}

object GetCelebrityProductEndpoint {
  /**
   * Reverses the product index url for a particular celebrity and product
   */
  def url(celebrity:Celebrity, product:Product) = {
    val params: Map[String, AnyRef] = Map(
      "celebrityUrlSlug" -> celebrity.urlSlug.get,
      "productUrlSlug" -> product.urlSlug
    )

    Utils.lookupUrl("WebsiteControllers.getCelebrityProducts", params)
  }
}





