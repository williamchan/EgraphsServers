package controllers.website

import play.mvc.Controller
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import models.frontend.contents.Section
import controllers.WebsiteControllers

private[controllers] trait GetStaticEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  def getAbout = controllerMethod() {
    views.frontend.html.about_us(learnMoreUrl = WebsiteControllers.reverse(WebsiteControllers.getInsideEgraph).url)
  }

  def getCareers = controllerMethod() {
    views.frontend.html.careers()
  }

  def getFAQ = controllerMethod() {
    views.frontend.html.faq()
  }

  def getInsideEgraph = controllerMethod() {
    val tableOfContents =
      List(
        Section(title="Introduction", url="#inside", subsection = None),
        Section(title="What is an egraph?", url="#what", subsection = None),
        Section(title="The biometric authentication process", url="#biometric", subsection = None),
        Section(title="Enjoying and sharing your egraph", url="#do", subsection = None)
      )
    views.frontend.html.inside_egraph(tableOfContents)
  }


  def getPrivacy = controllerMethod() {
    views.frontend.html.privacy()
  }

  def getTerms = controllerMethod() {
    views.frontend.html.terms()
  }
}
