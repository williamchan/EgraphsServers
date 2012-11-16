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
import models.frontend.marketplace.CelebritySortingTypes

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

  private def sortOptionViewModels(selectedSortingType: Option[CelebritySortingTypes.EnumVal] = None) : Iterable[SortOptionViewModel] = {
    for {
      sortingType <- CelebritySortingTypes.values
    } yield {
      SortOptionViewModel(
        name = sortingType.name,
        display = sortingType.displayName,
        active = (Some(sortingType) == selectedSortingType))
    }
  }
  /**
   * Serves up marketplace results page. If there are no query arguments, featured celebs are served. 
   **/
  def getMarketplaceResultPage = controllerMethod.withForm() { implicit AuthToken => 
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val verticalOption = Form("vertical" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val queryOption = Form("query" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val maybeSortType = Form("sort" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, sortOption => CelebritySortingTypes(sortOption))
      val viewOption = Form("view" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))

      val categoryAndCategoryValues = 
        for((key, set) <- request.queryString; categoryRegex(id) <- categoryRegex findFirstIn key) yield {
          //TODO ensure this set can become a long
          (id.toLong, set.map( arg => arg.toLong))
        }
      
      val activeCategoryValues = {for{
        (category, categoryValues) <- categoryAndCategoryValues
        categoryValue <- categoryValues
      } yield { categoryValue }}.toSet
     
      val (subtitle, celebrities) = 
        (queryOption match {
          case Some(query) => ("Showing Results for \"" + query + "\"...", celebrityStore.marketplaceSearch(query, categoryAndCategoryValues, maybeSortType.getOrElse(CelebritySortingTypes.MostPopular)))
          case _ =>
            if(!activeCategoryValues.isEmpty) {
              ("Results", celebrityStore.marketplaceSearch("*", categoryAndCategoryValues, maybeSortType.getOrElse(CelebritySortingTypes.MostPopular)))
            } else {
              //TODO when refinements are implemented we can do this using tags instead.  
              ("Featured Celebrities" , celebrityStore.getFeaturedPublishedCelebrities.map{c => 

                val activeProductsAndInventory = c.getActiveProductsWithInventoryRemaining()
                val purchaseableProducts = activeProductsAndInventory.filter {
                  productAndCount => productAndCount._2 > 0
                }
                val prices = purchaseableProducts.map(p => p._1.priceInCurrency.toInt)
                c.asMarketplaceCelebrity(prices.min, prices.max, purchaseableProducts.isEmpty)
              })
            }
        })

      val viewAsList = viewOption == Some("list")

      //HACK As long as no CategoryValues have children Categories, this call ca be used to display
      // only baseball categories. This NEEDS to be fixed if we want to support multiple verticals. 
      val categoryValues = categoryValueStore.all().toList

      val categoryViewModels = for {
        categoryValue <- categoryValues
        category <- categoryValue.categories
      } yield {
        CategoryViewModel(
          id = category.id,
          publicName = category.publicName,
          categoryValues = category.categoryValues.map( cv =>
            CategoryValueViewModel(
              publicName = cv.publicName,
              id = cv.id,
              active = activeCategoryValues.contains(cv.id)
            )
          )
        )
      }

      Ok(views.html.frontend.marketplace_results(
        query = queryOption.getOrElse(""),
        viewAsList = viewAsList,
        marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
        verticalViewModels = getVerticals(activeCategoryValues),
        results = List(ResultSetViewModel(subtitle = Option(subtitle), celebrities)),
        categoryViewModels = categoryViewModels,
        sortOptions = sortOptionViewModels(maybeSortType)
      ))
    }
  }
  
  private def getVerticals(activeCategoryValues: Set[Long] = Set()) : List[VerticalViewModel] = {
    val categoryValues = categoryValueStore.all().toList
// TODO manage these verticals properly.     
    List()
  }

    // TODO implement landing pages for verticals. 
  // def getMarketplaceVerticalPage(verticalname: String) =  controllerMethod.withForm() { implicit authToken =>
  //   Action { implicit request =>
  //    //Map Vertical to specific landing page.
  //      Ok(views.html.frontend.marketplace_landing(
  //        //TODO Fix this queryUrl thing 
  //          queryUrl = "",
  //        marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url,
  //        verticalViewModels = getVerticals(),
  //        results = List[ResultSetViewModel](),
  //        categoryViewModels = List[CategoryViewModel](),
  //        sortOptions = sortOptionViewModels() 
  //      ))     
  //   }
  // }
}
