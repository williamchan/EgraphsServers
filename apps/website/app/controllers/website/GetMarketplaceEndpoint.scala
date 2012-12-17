package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import models._
import play.api.data._
import play.api.data.Forms._
import models.categories._
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import services.http.ControllerMethod
import models.frontend.marketplace._
import models.frontend.marketplace.CelebritySortingTypes
import services.db.TransactionSerializable
import services.db.DBSession
import celebrity.CatalogStarsQuery
import CelebritySortingTypes._
import play.api.mvc.AnyContent
import play.api.mvc.Request

/**
 * Controller for serving the celebrity marketplace
 */

private[controllers] trait GetMarketplaceEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod : ControllerMethod
  protected def celebrityStore : CelebrityStore  
  protected def categoryValueStore: CategoryValueStore
  protected def catalogStarsQuery: CatalogStarsQuery
  protected def dbSession: DBSession
  protected def featured: Featured
  protected def verticalStore: VerticalStore

  val queryUrl = controllers.routes.WebsiteControllers.getMarketplaceResultPage("").url
  val categoryRegex = new scala.util.matching.Regex("""c([0-9]+)""", "id")

  private def sortOptionViewModels(selectedSortingType: Option[CelebritySortingTypes.EnumVal] = None) : Iterable[SortOptionViewModel] = {
    for {
      sortingType <- CelebritySortingTypes.values
    } yield {
      SortOptionViewModel(
        name = sortingType.name,
        display = sortingType.displayName,
        active = (sortingType == selectedSortingType.getOrElse(CelebritySortingTypes.MostRelevant)))
    }
  }

  private def parseCategoryValues(implicit request: Request[AnyContent]) : Map[Long, Seq[Long]] = {
    for {
      (key, set) <- request.queryString
      categoryRegex(id) <- categoryRegex findFirstIn key
    } yield {
      val categoryValueIds = set.map(arg =>
        try {
          arg.toLong
        } catch {
          case e: Exception => -1 // C Style
        })
      (id.toLong, categoryValueIds.filter(_ > -1))
    }
  }

  private def getVerticalViewModels(maybeSelectedVertical: Option[Vertical], activeCategoryValues: Set[Long]) : List[VerticalViewModel] = {
    verticalStore.verticals.map { v =>
      val categories = for {
        category <- v.categories
      } yield {
        CategoryViewModel(
          id = category.id,
          publicName = category.publicName,
          // TODO(sbilstein) think about making this more efficient.
          categoryValues = category.categoryValues.map( cv =>
            CategoryValueViewModel(
              publicName = cv.publicName,
              id = cv.id,
              active = activeCategoryValues.contains(cv.id)
            )
          ).toList)
      }
      VerticalViewModel(
        verticalName = v.categoryValue.name,
        publicName = v.categoryValue.publicName,
        shortName = v.shortName,
        urlSlug = v.urlSlug,
        iconUrl = v.iconUrl,
        active = v.urlSlug == maybeSelectedVertical.map(_.urlSlug).getOrElse(""),
        id = v.categoryValue.id,
        categoryViewModels = categories
      )
    }.toList
  }

  private def buildSubtitle(queryOption: Option[String], celebrities: Iterable[MarketplaceCelebrity]) : String = {
    queryOption match {
      case None => celebrities.size match {
        case 1 => "Showing 1 Result"
        case _ => "Showing " + celebrities.size + " Results"
      }
      case Some(query) => {
        celebrities.size match {
          case 1 => "Showing 1 Result for \"" + query + "\"..."
          case _ => "Showing " + celebrities.size + " Results for \"" + query + "\"..."
        }
      }
    }
  }

  private def sortCelebrities(sortingType : CelebritySortingTypes.EnumVal, unsortedCelebrities: Iterable[MarketplaceCelebrity]) :
  Iterable[MarketplaceCelebrity] = {
    sortingType match {
      case MostRelevant => unsortedCelebrities
      case PriceAscending => unsortedCelebrities.toList.sortWith((a,b) => a.minPrice < b.minPrice)
      case PriceDescending => unsortedCelebrities.toList.sortWith((a,b) => a.maxPrice > b.maxPrice)
      case Alphabetical => unsortedCelebrities.toList.sortWith((a,b) => a.publicName < b.publicName)
      case _ => unsortedCelebrities
    }
  }
  /**
   * This controller serves up the marketplace.
   * When no state is passed (query args or a vertical name) the landing page is served.
   * Otherwise, this page distills various arguments into a query and prepares results to be served.
   * @param vertical The slug of a marketplace search if it is scoped by vertical
   * @return A results page or landing page.
   */
  def getMarketplaceResultPage(vertical : String = "") = controllerMethod.withForm() { implicit AuthToken =>
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val marketplaceResultPageForm = Form(
        tuple(
          "query" -> optional(nonEmptyText), // User submitted search
          "sort" -> optional(nonEmptyText),  // Ordering of results
          "view" -> optional(nonEmptyText),  // Grid view or list view
          "availableOnly" -> optional(boolean) // If true, only serve stars that are NOT sold out
        )
      )
      // Process the form
      val maybeSelectedVertical =  verticalStore.verticals.filter(v => v.urlSlug == vertical).headOption
      val (queryOption, sortOption, viewOption, availableOnlyOption) = marketplaceResultPageForm.bindFromRequest.fold(
        errors => (None, None, None, None),
        formOptions => formOptions
      )
      val maybeSortType = sortOption.flatMap(sort => CelebritySortingTypes(sort))
      val availableOnly = availableOnlyOption.getOrElse(false)
      val categoryAndCategoryValues = parseCategoryValues

      // Refinements to pass to the search function
      val categoryValuesRefinements = for ((category, categoryValues) <- categoryAndCategoryValues) yield categoryValues

      // If the search is scoped to a vertical, include the vertical as a category value
      val verticalAndCategoryValues = maybeSelectedVertical match {
        case Some(vertical) => categoryValuesRefinements ++ List(Seq(vertical.categoryValue.id))
        case None => categoryValuesRefinements
      }
      // Yield a list of selected category values
      val activeCategoryValues = {
        for {
          (category, categoryValues) <- categoryAndCategoryValues
          categoryValue <- categoryValues
        } yield { categoryValue }
      }.toSet

      val verticalViewModels = getVerticalViewModels(maybeSelectedVertical, activeCategoryValues)

      // Check if any search options have been defined
      if(queryOption.isDefined || !verticalAndCategoryValues.isEmpty) {
        // Serve results according to the query
        val unsortedCelebrities = dbSession.connected(TransactionSerializable) {
          celebrityStore.marketplaceSearch(queryOption, verticalAndCategoryValues)
        }

        val sortedCelebrities = sortCelebrities(maybeSortType.getOrElse(CelebritySortingTypes.MostRelevant), unsortedCelebrities)

        val celebrities = if(availableOnly){
          sortedCelebrities.filter(c => !c.soldout)
        } else {
          sortedCelebrities
        }

        val subtitle = buildSubtitle(queryOption, celebrities)

        val viewAsList = viewOption == Some("list") //TODO "list" should be a part of an Enum

        Ok(views.html.frontend.marketplace_results(
          query = queryOption.getOrElse(""),
          viewAsList = viewAsList,
          marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage("").url,
          verticalViewModels = verticalViewModels,
          results = ResultSetViewModel(subtitle = Option(subtitle), verticalUrl = Option("/"), celebrities = celebrities),
          sortOptions = sortOptionViewModels(maybeSortType),
          availableOnly = availableOnly
        ))
      } else {
        // No search options so serve the landing page. Every vertical has a category value which decides what to
        // display here. Limit to three results to keep both verticals above the fold. What is a newspaper?
        val resultSets = for(vertical <- verticalStore.verticals) yield {
          val categoryValue = vertical.categoryValue
          val results = celebrityStore.marketplaceSearch(Option(vertical.featuredQuery), List(Seq(categoryValue.id)))
          ResultSetViewModel(subtitle = Option(categoryValue.publicName),
                             verticalUrl = Option(vertical.urlSlug),
                             celebrities = results.slice(0,3))
        }
        // Serve the landing page.
        Ok(views.html.frontend.marketplace_landing(
          marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage("").url,
          verticalViewModels = verticalViewModels,
          resultSets = resultSets.toList
        ))
      }
    }
  }
}