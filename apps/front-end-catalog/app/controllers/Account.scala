package controllers

import play.mvc.Controller


object Account extends Controller {

  def settings() = {
    views.frontend.html.account_settings()
  }

}