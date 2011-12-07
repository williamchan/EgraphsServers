package controllers

import play.mvc.{Router, Controller}
import models.{Celebrity, Product}
import libs.Utils

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

  def buy(recipientName: String, recipientEmail: String) = {
    println("Got the post!")
    println("params were --"+params)
    
    "Gotcha!"
  }

  /**
   * Reverses the product index url for a particular celebrity and product
   */
  def indexUrl(celebrity:Celebrity, product:Product) = {
    val params: Map[String, AnyRef] = Map(
      "celebrityUrlSlug" -> celebrity.urlSlug,
      "productUrlSlug" -> product.urlSlug
    )

    Utils.lookupUrl("CelebrityProductController.index", params)
  }
}