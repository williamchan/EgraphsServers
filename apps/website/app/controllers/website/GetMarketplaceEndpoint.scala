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
  
  val sortFunctions = 
    Map("recently-added" -> "Recently Added",
        "most-popular" -> "Most Popular",
        "price-low-to-hi" -> "Price (Low to High)",
        "price-high-to-low" -> "Price (High to Low)",
        "alphabetical-a-z"  -> "Alphabetical (A-Z)",
        "alphabetical-z-a" -> "Alphabetical (Z-A)"
    )
  private def sortOptionViewModels(selected: String = "") : Iterable[SortOptionViewModel] = {
    sortFunctions.map( (sortFunction) => 
      SortOptionViewModel(name = sortFunction._1, display = sortFunction._2, active = (sortFunction._1 == selected))
    )
  }  
  
  def getMarketplaceVerticalPage(verticalname: String) =  controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
     //Map Vertical to specific landing page.
       Ok(views.html.frontend.marketplace_landing(
         //TODO Fix this queryUrl thing 
           queryUrl = "",
         marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
         verticalViewModels = getVerticals(),
         results = List[ResultSetViewModel](),
         categoryViewModels = List[CategoryViewModel](),
         sortOptions = sortOptionViewModels() 
       ))
      
    }
  }

  def getMarketplaceResultPage = controllerMethod.withForm() { implicit AuthToken => 
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val verticalOption = Form("vertical" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val queryOption = Form("query" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val sortOption = Form("sort" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val viewOption = Form("view" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))

      val categoryAndCategoryValues = 
        for((key, set) <- request.queryString; categoryRegex(id) <- categoryRegex findFirstIn key) yield {
          //TODO ensure this set can become a long
          (id.toLong, set.map( arg => arg.toLong))
        }
      
      val activeCategoryValues = categoryAndCategoryValues.foldLeft[Set[Long]](Set[Long]())((set, ccv) => set ++ ccv._2.toSet[Long])
      
      val (subtitle, celebrities) = 
        (queryOption match {
          case Some(query) => ("Showing Results for \"" + query + "\"...", celebrityStore.marketplaceSearch(query, categoryAndCategoryValues))
          case _ => activeCategoryValues.isEmpty match {
            case false => ("Results", celebrityStore.marketplaceSearch("*", categoryAndCategoryValues))
            case true => ("Featured Celebrities" , celebrityStore.getFeaturedPublishedCelebrities.map(c => c.asMarketplaceCelebrity(100,100, true)))
          }
        })

      val viewAsList = viewOption match {
        case Some(view) if(view == "list") => true
        case _ => false
      }  

                
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
         viewAsList = viewAsList,
         marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
         verticalViewModels = getVerticals(activeCategoryValues),
         results = List(ResultSetViewModel(subtitle=Option(subtitle), celebrities)),
         categoryViewModels = categoryViewModels,
         sortOptions = sortOptionViewModels(sortOption.getOrElse(""))
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
       id = 4,
       active =  true
      )    
    )
  }
}