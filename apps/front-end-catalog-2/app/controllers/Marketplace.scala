package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import models.frontend.contents.Section
import helpers.DefaultImplicitTemplateParameters

/**
 * Marketplace controller
 */
object Marketplace extends Controller with DefaultImplicitTemplateParameters {

  def index() = Action {
    Ok(views.html.frontend.marketplace_landing())
  }
  
}
