package controllers.website.consumer

import play.api._
import play.api.mvc.Action
import play.api.mvc.Controller
import models._
import models.frontend.marketplace._
import play.api.data._
import play.api.data.Forms._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, EgraphState}
import models.frontend.egraphs._
import services.mvc.ImplicitHeaderAndFooterData
import models.GalleryOrderFactory
import services.ConsumerApplication
import services.http.{SafePlayParams, ControllerMethod}
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import egraphs.authtoken.AuthenticityToken
import services.mvc.celebrity.CelebrityViewConversions

/**
 * Controller for serving the celebrity marketplace
 */

private[controllers] trait GetMarketplaceEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod : ControllerMethod
  protected def celebrityStore : CelebrityStore  
  import CelebrityViewConversions._
  
  import SafePlayParams.Conversions._
  val queryUrl = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url
  val categoryRegex = new scala.util.matching.Regex("""c([0-9])""", "id")
  
  def getMarketplaceVerticalPage(verticalname: String) =  controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
     //Map Vertical to specific landing page.
       Ok(views.html.frontend.marketplace_landing(
         queryUrl = queryUrl.toString,
         marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
         verticalViewModels = List[VerticalViewModel](),
         results = List[ResultSetViewModel](),
         categoryViewModels = List[CategoryViewModel]()
       ))
      
    }
  }
    
  def getMarketplaceResultPage = controllerMethod.withForm() { implicit AuthToken => 
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val verticalOption = Form("vertical" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val queryOption = Form("query" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))

      val categoryAndCategoryValues  = for((key, set) <- request.queryString) yield {
        categoryRegex findFirstIn key match {
          case Some(categoryRegex(id)) => (id, set)
          case None => None
        }
      }  
      
      /**
       * The set of refinements has the following schema:
       * Set[CategoryId, Iterable[CategoryValueId]]
       * This can be viewed as a mapping of categories to selected tags.
       * 
       * The refinements should be processed like this:
       * 
       * (CategoryValueId OR CategoryValueId) AND (CatgeoryValueId OR CategoryValueId OR CategoryValueId) AND ...
       * for each set of CategoryValueIds through the CelebrityCategoryValues table that defines how celebrities are tagged.
       * 
       * A real world example:
       * A user is looking at results that have the CategoryValue "Boston Red Sox" selected (as part of Category "Teams")
       * She decides she wants to see only pitchers and clicks Pitchers in the Category "Positions"
       * Server recieves a request that is processed with categoryAndCategoryValues that looks like this (with the implementation being numerical ids, not strings
       *  ["Team" -> ["Boston Red Sox"], "Position" -> ["Pitcher"]]
       *  
       * User then decides they are also interested in third basemen, the next request will look like this and show any boston pitcher or third basemen
       * ["Team" -> ["Boston Red Sox"], "Position" -> ["Pitcher", "Third Basemen"]]
       * 
       * User decides she doesn't care about boston, next request looks like this and should return all pitchers and third basemen
       * ["Position" -> ["Pitcher", "Third Basemen"]]
       * 
       */
      // 3) No results, send something
      // 4) Error in data, same as no results but with error message
      val celebrities = 
        (queryOption match {
          case Some(query) => celebrityStore.marketplaceSearch(query)
          case _ => List()
        }) 
        
      val subtitle = queryOption match {
        case Some(query) => "Results for " + query + "..."
        case None => "Results" //todo something meaningful
      }
      
       Ok(views.html.frontend.marketplace_results(
         queryUrl = queryUrl.toString,
         marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
         verticalViewModels = List[VerticalViewModel](),
         results = List(ResultSetViewModel(subtitle=Option(subtitle), celebrities)),
         categoryViewModels = List[CategoryViewModel]()
       ))
    }
  }

}