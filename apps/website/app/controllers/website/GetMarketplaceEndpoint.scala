package controllers.website.consumer

import play.api._
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.Play.current
import models._
import models.frontend.marketplace._
import play.api.data._
import play.api.data.Forms._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, EgraphState}
import models.frontend.egraphs._
import models.categories._
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import models.GalleryOrderFactory
import services.ConsumerApplication
import services.http.{SafePlayParams, ControllerMethod}
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import egraphs.authtoken.AuthenticityToken
import services.mvc.celebrity.CelebrityViewConversions
import models.frontend.marketplace._
import models.frontend.marketplace.CelebritySortingTypes
import play.api.libs.concurrent.Akka
import services.db.TransactionSerializable
import services.db.DBSession
import celebrity.CatalogStarsQuery

/**
 * Controller for serving the celebrity marketplace
 */

private[controllers] trait GetMarketplaceEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod : ControllerMethod
  protected def celebrityStore : CelebrityStore  
  protected def categoryValueStore: CategoryValueStore
  protected def catalogStarsQuery: CatalogStarsQuery
  protected def dbSession: DBSession
  
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
      val marketplaceResultPageForm = Form(
        tuple(
          "vertical" -> optional(nonEmptyText),
          "query" -> optional(nonEmptyText),
          "sort" -> optional(nonEmptyText),
          "view" -> optional(nonEmptyText))
        )

      val (verticalIdOption, queryOption, sortOption, viewOption) = marketplaceResultPageForm.bindFromRequest.fold(
        formWithErrors => throw new Exception("This shouldn't be possible")
        ,{
          case (verticalIdOption, queryOption, sortOption, viewOption) =>
            (verticalIdOption, queryOption, sortOption, viewOption)
        })

      val maybeSortType = sortOption match {
        case Some(sort) => CelebritySortingTypes(sort)
        case _ => None
      }

      val categoryAndCategoryValues = 
        for((key, set) <- request.queryString; categoryRegex(id) <- categoryRegex findFirstIn key) yield {
          //TODO ensure this set can become a long
          (id.toLong, set.map( arg => arg.toLong))
        }
      
      val activeCategoryValues = {for{
        (category, categoryValues) <- categoryAndCategoryValues
        categoryValue <- categoryValues
      } yield { categoryValue }}.toSet

      val (subtitle, celebrities) = dbSession.connected(TransactionSerializable) {
        queryOption match {
          case Some(query) =>
            val results = celebrityStore.marketplaceSearch(queryOption, categoryAndCategoryValues, maybeSortType.getOrElse(CelebritySortingTypes.MostRelevant))
            val text = results.size match {
              case 1 => "Showing 1 Result for \"" + query + "\"..."
              case _ => "Showing " + results.size + " Results for \"" + query + "\"..."
            }
            (text, results)
          case _ =>
            if (!activeCategoryValues.isEmpty) {
              ("Results", celebrityStore.marketplaceSearch(queryOption, categoryAndCategoryValues, maybeSortType.getOrElse(CelebritySortingTypes.MostRelevant)))
            } else {
              //TODO when refinements are implemented we can do this using tags instead.  
              ("Featured Stars", catalogStarsQuery().filter(star => star.isFeatured).map(c =>
                MarketplaceCelebrity(
                  id = c.id,
                  publicName = c.name,
                  photoUrl = c.marketplaceImageUrl,
                  storefrontUrl = c.storefrontUrl,
                  soldout = !c.hasInventoryRemaining,
                  minPrice = c.minPrice,
                  maxPrice = c.maxPrice,
                  secondaryText = c.secondaryText.getOrElse(""))))
            }
        }
      }

      val viewAsList = viewOption == Some("list") // "list" should be a part of an Enum

      println("~~~~~~~~~~~~~~~~~~~~~~~~~" + verticalIdOption)
      
      //HACK As long as no CategoryValues have children Categories, this call can be used to display
      // only baseball categories. This NEEDS to be fixed if we want to support multiple verticals.
      //TODO: This block is slow as fuck, fix it
      val categoryViewModels = for {
        categoryValue <- categoryValueStore.all().toList
        category <- categoryValue.categories
      } yield {
        println(categoryValue + "~~~~~~~~~~~~~~" + category)
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
        sortOptions = sortOptionViewModels(maybeSortType)))
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
