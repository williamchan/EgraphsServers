package controllers

import play.mvc.Controller
import models.frontend.contents.Section

/**
 * Marketplace controller
 */

object Marketplace extends Controller with DefaultHeaderAndFooterData {

  def index() = {
    views.frontend.html.marketplace_landing()
  }
  
}
