package controllers.website

import play.api._
import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import models.frontend.contents.Section

private[controllers] trait GetStaticEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  def getAbout = controllerMethod() {
    Action { implicit request =>
      Ok(views.html.frontend.about_us(learnMoreUrl = controllers.routes.WebsiteControllers.getInsideEgraph().url))
    }
  }

  def getCareers = controllerMethod() {
    Action { implicit request =>
      Ok(views.html.frontend.careers())
    }
  }

  def getFAQ = controllerMethod() {
    Action { implicit request =>
      Ok(views.html.frontend.faq())
    }
  }

  def getInsideEgraph = controllerMethod() {
    Action { implicit request =>
      val tableOfContents =
        List(
          Section(title="Introduction", url="#inside", subsection = None),
          Section(title="What is an egraph?", url="#what", subsection = None),
          Section(title="The biometric authentication process", url="#biometric", subsection = None),
          Section(title="Enjoying and sharing your egraph", url="#do", subsection = None)
        )
      Ok(views.html.frontend.inside_egraph(tableOfContents))
    }
  }  

  def getPrivacy = controllerMethod() {
    Action { implicit request =>
      Ok(views.html.frontend.privacy())
    }
  }  

  def getInsiderSweepstakes = controllerMethod() {
    Action { implicit request =>
      Ok(views.html.frontend.sweepstakes_insider())
    }
  }  

  def getTerms = controllerMethod() {
    Action { implicit request =>
      Ok(views.html.frontend.terms())
    }
  }  
}
