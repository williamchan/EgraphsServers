package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import models._
import play.api.data._
import play.api.data.Forms._
import models.categories._
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import services.mvc.marketplace.MarketplaceServices
import services.http.ControllerMethod
import services.http.EgraphsSession._
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import models.frontend.marketplace._
import models.frontend.marketplace.CelebritySortingTypes
import services.db.TransactionSerializable
import services.db.DBSession
import celebrity.CatalogStarsQuery
import CelebritySortingTypes._
import play.api.mvc.AnyContent
import play.api.mvc.Request
import controllers.website.consumer.PostRequestStarEndpoint
import egraphs.playutils.FlashableForm._

/**
 * Controller for serving the celebrity marketplace
 */
private[controllers] trait GetMarketplaceEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def catalogStarsQuery: CatalogStarsQuery
  protected def categoryValueStore: CategoryValueStore
  protected def celebrityRequestStore: CelebrityRequestStore
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod
  protected def dbSession: DBSession
  protected def featured: Featured
  protected def httpFilters: HttpFilters
  protected def marketplaceServices: MarketplaceServices
  protected def verticalStore: VerticalStore

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
      case MostRelevant =>
        // by twitter follower count desc
        import services.mvc.celebrity.TwitterFollowersAgent
        val celebrityIdToFollowersCount = TwitterFollowersAgent.singleton().withDefaultValue(0)
        unsortedCelebrities.toList.sortWith((a,b) => celebrityIdToFollowersCount(a.id) > celebrityIdToFollowersCount(b.id))
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
  def getMarketplaceResultPage(vertical: String = "", query: Option[String] = None) = controllerMethod.withForm() { implicit AuthToken =>
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
      val (_, sortOption, viewOption, availableOnlyOption) = marketplaceResultPageForm.bindFromRequest.fold(
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

      val verticalViewModels = marketplaceServices.getVerticalViewModels(maybeSelectedVertical, activeCategoryValues)

      // Check if any search options have been defined
      if(!query.isEmpty || !verticalAndCategoryValues.isEmpty) {
        // Serve results according to the query
        val unsortedCelebrities = dbSession.connected(TransactionSerializable) {
          celebrityStore.marketplaceSearch(query, verticalAndCategoryValues)
        }

        val sortedCelebrities = sortCelebrities(maybeSortType.getOrElse(CelebritySortingTypes.MostRelevant), unsortedCelebrities)

        val celebrities = if(availableOnly){
          sortedCelebrities.filter(c => !c.soldout)
        } else {
          sortedCelebrities
        }

        val subtitle = buildSubtitle(query, celebrities)

        val viewAsList = viewOption == Some("list") //TODO "list" should be a part of an Enum

        val marketplaceTargetUrl = controllers.routes.WebsiteControllers.getMarketplaceResultPage("", query).url

        // true if customer is logged in and has already requested this same celebrity
        val hasAlreadyRequested = getHasAlreadyRequested(query.getOrElse(""))

        Ok(views.html.frontend.marketplace_results(
          query = query.getOrElse(""),
          viewAsList = viewAsList,
          marketplaceRoute = marketplaceTargetUrl,
          verticalViewModels = verticalViewModels,
          results = ResultSetViewModel(subtitle = Option(subtitle), verticalUrl = Option("/"), celebrities = celebrities),
          sortOptions = sortOptionViewModels(maybeSortType),
          availableOnly = availableOnly,
          requestStarForm = PostRequestStarEndpoint.form.bindWithFlashData,
          requestStarActionUrl = controllers.routes.WebsiteControllers.postRequestStar.url,
          hasAlreadyRequested
        ))
        .withSession(request.session.withAfterLoginRedirectUrl(marketplaceTargetUrl))

      } else {
        // No search options so serve the landing page. If a vertical has a category value which feature stars, it is
        // displayed via this query.  Limit to three results to keep verticals above the fold.
        val resultSets =
          for(vertical <- verticalStore.verticals;
             featuredQuery <- vertical.featuredQuery) yield {
            val categoryValue = vertical.categoryValue
            val results = celebrityStore.marketplaceSearch(Option(featuredQuery), List(Seq(categoryValue.id)))
            ResultSetViewModel(subtitle = Option(categoryValue.publicName),
                               verticalUrl = Option(vertical.urlSlug),
                               celebrities = results.slice(0,3))
        }
        // Serve the landing page.
        Ok(views.html.frontend.marketplace_landing(
          marketplaceRoute = controllers.routes.WebsiteControllers.getMarketplaceResultPage("", None).url,
          verticalViewModels = verticalViewModels,
          resultSets = resultSets.toList
        ))
      }
    }
  }

  private def getHasAlreadyRequested(query: String)(implicit request: Request[AnyContent]): Boolean = {
    val eitherCustomerAndAccountOrResult = httpFilters.requireCustomerLogin.filterInSession()
    eitherCustomerAndAccountOrResult match {
      case Right((customer, account)) => {
        val maybeCelebrityRequest = celebrityRequestStore.getCelebrityRequestByCustomerIdAndCelebrityName(
          customer.id, query)

        maybeCelebrityRequest.isDefined
      }
      case Left(result) => false
    }
  }
}