package controllers

import play.mvc.{Router, Controller}
import models.{Celebrity, Product}

/**
 * Serves pages relating to a particular product of a celebrity.
 */
object CelebrityProductController extends Controller
  with DBTransaction
  with RequiresCelebrityName
  with RequiresCelebrityProductName
{

  def index = {
    views.Application.html.product(celebrity, product)
  }

  /**
   * Reverses the product index url for a particular celebrity and product
   */
  def indexUrl(celebrity:Celebrity, product:Product) = {
    import scala.collection.JavaConversions._

    val params: Map[String, Object] = Map(
      "celebrityUrlSlug" -> celebrity.urlSlug,
      "productUrlSlug" -> product.urlSlug
    )

    Router.reverse("CelebrityProductController.index", params)
  }
}