package controllers.website.consumer

import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.ControllerMethod
import services.mvc.{ celebrity, ImplicitHeaderAndFooterData }
import models.CelebrityStore
import models.categories.{ VerticalStore, Featured }
import services.mvc.marketplace._
import services.http.SignupModal
import services.mvc.landing.LandingMastheadsQuery

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
  protected def signupModal: SignupModal
  protected def landingMastheadsQuery: LandingMastheadsQuery
  //
  // Controllers
  //

  var marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage("").url

  def getRootConsumerEndpoint = controllerMethod.withForm() { implicit authToken =>
    signupModal() { displayModal =>
      Action { implicit request =>
        val featuredStars = celebrityStore.catalogStarsSearch(refinements = List(List(featured.categoryValue.id)))
        val verticals = marketplaceServices.getVerticalViewModels()
        val displayModal = request.queryString.get("signup") match {
          case Some(Seq("true")) => true
          case _ => signupModal.shouldDisplay(request)
        }

        Ok(views.html.frontend.landing(
          stars = featuredStars.toList,
          verticalViewModels = verticals,
          marketplaceRoute = marketplaceRoute,
          signup = displayModal))
      }
    }
  }
}
