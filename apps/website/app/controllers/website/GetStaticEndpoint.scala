package controllers.website

import play.mvc.Controller
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import models.frontend.contents.Section
import services.Utils

private[controllers] trait GetStaticEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  def getAbout = controllerMethod() {
    views.frontend.html.about_us(learnMoreUrl = Utils.lookupUrl("WebsiteControllers.getInsideEgraph").url)
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
        Section(title="Sharing an egraph", url="#do", subsection = None)
      )
    views.frontend.html.inside_egraph(tableOfContents)
  }

}