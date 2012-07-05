package controllers

import play.mvc.Controller

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def index = {
    views.frontend.html.email_order_confirmation()
  }

  def verify = {
    views.frontend.html.email_account_verification()
  }

  def view_egraph = {
    views.frontend.html.email_view_egraph()
  }

}

