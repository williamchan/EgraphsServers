package controllers

import play.api.data._
import play.api.data.Forms._

import play.api.mvc.{Action, Controller}
import helpers.DefaultImplicitTemplateParameters

object StorefrontA extends Controller with DefaultImplicitTemplateParameters {
  def personalize = Action { request =>
    Ok(views.html.frontend.storefronts.a.personalize())
  }

  def checkout(testcase: Option[String]) = Action { request =>
    val request = Form
    Ok(views.html.frontend.storefronts.a.checkout(testcase=testcase))
  }
}
