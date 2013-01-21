package controllers

import play.api.mvc.{Action, Controller}
import helpers.DefaultImplicitTemplateParameters

object StorefrontA extends Controller with DefaultImplicitTemplateParameters {
  def personalize = Action {
    Ok(views.html.frontend.storefronts.a.personalize())
  }
}
