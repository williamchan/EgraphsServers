package controllers

import play.mvc.Controller
import models.frontend.contents.Section

/**
 * Test controller for any generic static pages
 */

object Static extends Controller with DefaultHeaderAndFooterData {
  def simple_confirmation() = {
    views.html.frontend.simple_confirmation("Account Verified",
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
    views.html.frontend.about_us("/inside-an-egraph")
  }

  def inside() = {
    val tableOfContents =
    List(
      Section(title="Introduction", url="#inside", subsection = None),
      Section(title="What is an Egraph?", url="#what", subsection = None),
      Section(title="The Biometric Authentication Process", url="#biometric", subsection = None),
      Section(title="What Can I Do With My Egraph", url="#do", subsection = None)
    )

    views.html.frontend.inside_egraph(tableOfContents)
  }

  def terms() = {
    views.html.frontend.terms()
  }

  def contact() = {
    views.html.frontend.contact()
  }

  def privacy() = {
    views.html.frontend.privacy()
  }

  def faq() = {
    views.html.frontend.faq()
  }

  def careers() = {
    views.frontend.html.careers()
  }
}
