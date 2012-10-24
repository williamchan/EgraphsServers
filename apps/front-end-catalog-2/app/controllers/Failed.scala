package controllers

import play.api._
import play.api.mvc._
import helpers.DefaultImplicitTemplateParameters

object Failed extends Controller with DefaultImplicitTemplateParameters {
  def no_inventory = Action {
    Ok(views.html.frontend.celebrity_storefront_no_inventory(
      celebrityName = "{celebrity name}",
      productName = "{product name}"
    ))
  }

  def creditcard_error = Action {
    Ok(views.html.frontend.celebrity_storefront_creditcard_error(
      celebrityName = "{celebrity name}",
      productName = "{product name}",
      creditCardMsg = "Your card was declined"
    ))
  }

  def purchase_error = Action {
    Ok(views.html.frontend.celebrity_storefront_purchase_error(
      celebrityName = "{celebrity name}",
      productName = "{product name}"
    ))
  }
}

