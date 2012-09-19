package controllers.website

import play.api._
import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import models.frontend.contents.Section
import controllers.WebsiteControllers

private[controllers] trait GetStaticEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  def getAbout = Action { implicit request =>
    controllerMethod() {
      Ok(views.html.frontend.about_us(learnMoreUrl = controllers.routes.WebsiteControllers.getInsideEgraph().url))
    }
  }

  def getCareers = Action { implicit request =>
    controllerMethod() {
      Ok(views.html.frontend.careers())
    }
  }

  def getFAQ = Action { implicit request =>
    controllerMethod() {
      Ok(views.html.frontend.faq())
    }
  }

  def getInsideEgraph = Action { implicit request =>
    controllerMethod() {
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

  def getPrivacy = Action { implicit request =>
    controllerMethod() {
      Ok(views.html.frontend.privacy())
    }
  }

  def getInsiderSweepstakes = Action { implicit request =>
    controllerMethod() {
      Ok(views.html.frontend.sweepstakes_insider())
    }
  }

  def getTerms = Action { implicit request =>
    controllerMethod() {
      Ok(views.html.frontend.terms())
    }
  }
}
