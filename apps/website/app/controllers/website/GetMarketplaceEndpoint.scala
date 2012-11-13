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
import models.categories._
import services.mvc.ImplicitHeaderAndFooterData
import models.GalleryOrderFactory
import services.ConsumerApplication
import services.http.{SafePlayParams, ControllerMethod}
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import egraphs.authtoken.AuthenticityToken
import services.mvc.celebrity.CelebrityViewConversions
import models.frontend.marketplace._

/**
 * Controller for serving the celebrity marketplace
 */

private[controllers] trait GetMarketplaceEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod : ControllerMethod
  protected def celebrityStore : CelebrityStore  
  protected def categoryValueStore: CategoryValueStore
  
  import CelebrityViewConversions._
  
  import SafePlayParams.Conversions._
  val queryUrl = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url
  val categoryRegex = new scala.util.matching.Regex("""c([0-9]+)""", "id")
  
  def getMarketplaceVerticalPage(verticalname: String) =  controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
     //Map Vertical to specific landing page.
       Ok(views.html.frontend.marketplace_landing(
         //TODO Fix this queryUrl thing
           queryUrl = "",
         marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
         verticalViewModels = getVerticals(),
         results = List[ResultSetViewModel](),
         categoryViewModels = List[CategoryViewModel]()
       ))
      
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
  
  def getMarketplaceResultPage = controllerMethod.withForm() { implicit AuthToken => 
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val verticalOption = Form("vertical" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val queryOption = Form("query" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))

      val categoryAndCategoryValues = 
        for((key, set) <- request.queryString; categoryRegex(id) <- categoryRegex findFirstIn key) yield {
          //TODO ensure this set can become a long
          (id.toLong, set.map( arg => arg.toLong))
        }
      
      val activeCategoryValues = categoryAndCategoryValues.foldLeft[Set[Long]](Set[Long]())((set, ccv) => set ++ ccv._2.toSet[Long])
      
      val (subtitle, celebrities) = 
        (queryOption match {
          case Some(query) => ("Results for \"" + query + "\"...", celebrityStore.marketplaceSearch(query, categoryAndCategoryValues))
          case _ => activeCategoryValues.isEmpty match {
            case false => ("Results", celebrityStore.marketplaceSearch("*", categoryAndCategoryValues))
            case true => ("Featured Celebrities" , celebrityStore.getFeaturedPublishedCelebrities.map(c => c.asMarketplaceCelebrity(100,100, true)))
          }
        })  
      val categoryViewModels = 
       categoryValueStore.findById(4).map( categoryValue => 
         categoryValue.categories.map ( c =>
           CategoryViewModel(
             id = c.id,
             publicName = c.publicName,
             categoryValues = c.categoryValues.map ( cv =>
               CategoryValueViewModel(
                 publicName = cv.publicName,
                 id  = cv.id,
                 active = activeCategoryValues.contains(cv.id)
               )
             )
           )
         )  
       ).getOrElse(List())
         
           
            
       Ok(views.html.frontend.marketplace_results(
         query = queryOption.getOrElse(""),
         marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
         verticalViewModels = getVerticals(activeCategoryValues),
         results = List(ResultSetViewModel(subtitle=Option(subtitle), celebrities)),
         categoryViewModels = categoryViewModels
       ))
    }
  }
  
  private def getVerticals(activeCategoryValues: Set[Long] = Set()) : List[VerticalViewModel] = {
    val verticalMlb = categoryValueStore.findById(4).get
    List(
      VerticalViewModel(
       verticalName = "Major-League-Baseball",
       publicName = verticalMlb.publicName,
       shortName = "MLB",
       iconUrl = "images/icon-logo-mlb.png",
       id = 4
      )    
    )
  }

}