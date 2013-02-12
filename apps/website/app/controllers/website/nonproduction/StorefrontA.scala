package controllers.website.nonproduction

import services.mvc.ImplicitHeaderAndFooterData
import services.logging.Logging
import play.api.mvc.{Action, Controller}
import services.AppConfig
import services.http.ControllerMethod
import services.http.filters.HttpFilters

trait StorefrontA extends Controller with Logging with ImplicitHeaderAndFooterData {
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def storefrontACheckout = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      val results = for (_ <- httpFilters.requireApplicationId.filter("hydrogen").right) yield {
        Ok(views.html.frontend.storefronts.a.checkout(testcase=Some("default")))
      }

      results.fold(error => error, success => success)
    }
  }
}
