package controllers.website.consumer

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.ControllerMethod
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import models.CelebrityStore
import models.categories.{VerticalStore, Featured}
import services.mvc.marketplace._
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
  protected def marketplaceServices: MarketplaceServices
  //
  // Controllers
  //

  var marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage("").url
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

  /**
   * Temporary controller for forking traffic between two designs. Serves the route /new. 
  **/

  def getRootConsumerEndpointA = controllerMethod.withForm() { implicit authToken =>
    Action {implicit request =>
      val featuredStars = celebrityStore.catalogStarsSearch(refinements = List(List(featured.categoryValue.id))).toList
      val verticals = marketplaceServices.getVerticalViewModels()
      val html = request.queryString.get("signup") match {
        case Some(Seq("true")) => views.html.frontend.landing_a(stars=featuredStars, verticalViewModels = verticals, marketplaceRoute = marketplaceRoute, signup = true)
        case _  =>  views.html.frontend.landing_a(stars=featuredStars, verticalViewModels = verticals, marketplaceRoute = marketplaceRoute, signup = false)
      }

      Ok(html)
    }
  }
}
