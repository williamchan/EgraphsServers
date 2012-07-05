package controllers

import play.mvc.Controller

/**
 * Test controller for any generic static pages
 */

object Static extends Controller with DefaultHeaderAndFooterData {
  def simple_confirmation() = {
    views.frontend.html.simple_confirmation("Account Verified",
      """
      Your account is now verified. Continue on to the rest of the <a href="/">Egraph's</a> website.
      <br>
      Thanks,
      <br>
      The team at Egraphs

      """
    )
  }

  def about() = {
    views.frontend.html.about_us()
  }
}
