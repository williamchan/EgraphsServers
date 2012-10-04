package controllers.website.consumer

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.ControllerMethod
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import celebrity.CatalogStarsQuery

/**
 * The main landing page for the consumer website.
 */
private[controllers] trait GetRootConsumerEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def catalogStarsQuery: CatalogStarsQuery

  //
  // Controllers
  //
  def getRootConsumerEndpoint = controllerMethod() {
    Action { implicit request =>
      val html = request.queryString.get("signup") match {
        case Some(Seq("true")) => views.html.frontend.landing(stars=catalogStarsQuery(), signup = true)
        case _  =>  views.html.frontend.landing(stars=catalogStarsQuery(), signup = false)
      }
      
      Ok(html)
    }
  }
}
