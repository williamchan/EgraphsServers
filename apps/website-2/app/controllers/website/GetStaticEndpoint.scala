package controllers.website

import play.api.mvc.Controller
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import models.frontend.contents.Section
import controllers.WebsiteControllers

private[controllers] trait GetStaticEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  def getAbout = controllerMethod() {
    views.html.frontend.about_us(learnMoreUrl = WebsiteControllers.reverse(WebsiteControllers.getInsideEgraph).url)
  }

  def getCareers = controllerMethod() {
    views.html.frontend.careers()
  }

  def getFAQ = controllerMethod() {
    views.html.frontend.faq()
  }

  def getInsideEgraph = controllerMethod() {
    val tableOfContents =
      List(
        Section(title="Introduction", url="#inside", subsection = None),
        Section(title="What is an egraph?", url="#what", subsection = None),
        Section(title="The biometric authentication process", url="#biometric", subsection = None),
        Section(title="Enjoying and sharing your egraph", url="#do", subsection = None)
      )
    views.html.frontend.inside_egraph(tableOfContents)
  }


  def getPrivacy = controllerMethod() {
    views.html.frontend.privacy()
  }

  def getInsiderSweepstakes = controllerMethod() {
    views.html.frontend.sweepstakes_insider()
  }

  def getTerms = controllerMethod() {
    views.html.frontend.terms()
  }
}
