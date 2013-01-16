package controllers.website.consumer

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.ControllerMethod
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import models.CelebrityStore
import models.categories.{VerticalStore, Featured}

/**
 * The main landing page for the consumer website.
 */
private[controllers] trait GetRootConsumerEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def celebrityStore: CelebrityStore
  protected def verticalStore: VerticalStore
  protected def featured: Featured

  //
  // Controllers
  //
  def getRootConsumerEndpoint = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      val featuredStars = celebrityStore.catalogStarsSearch(refinements = List(List(featured.categoryValue.id))).toList
      val html = request.queryString.get("signup") match {
        case Some(Seq("true")) => views.html.frontend.landing(stars=featuredStars, signup = true)
        case _  =>  views.html.frontend.landing(stars=featuredStars, signup = false)
      }
      
      Ok(html)
    }
  }

  def getRootConsumerAEndpoint = controllerMethod.withForm() { implicit authToken =>
    Action {implicit request =>
      val featuredStars = celebrityStore.catalogStarsSearch(refinements = List(List(featured.categoryValue.id))).toList
      val html = request.queryString.get("signup") match {
        case Some(Seq("true")) => views.html.frontend.landing_a(stars=featuredStars, signup = true)
        case _  =>  views.html.frontend.landing_a(stars=featuredStars, signup = false)
      }

      Ok(html)
    }
  }
}
