package controllers.website.consumer

import play.api.mvc.Action
import play.api.mvc.Controller
import models._
import play.api.data._
import play.api.data.Forms._
import models.categories._
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import services.http.{SafePlayParams, ControllerMethod}
import models.frontend.marketplace._
import models.frontend.marketplace.CelebritySortingTypes
import services.db.TransactionSerializable
import services.db.DBSession
import celebrity.CatalogStarsQuery
import CelebritySortingTypes._

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

  /**
   * Serves up marketplace results page. If there are no query arguments, featured celebs are served. 
   **/
  def getMarketplaceResultPage(vertical : String = "") = controllerMethod.withForm() { implicit AuthToken =>
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val marketplaceResultPageForm = Form(
        tuple(
          "query" -> optional(nonEmptyText),
          "sort" -> optional(nonEmptyText),
          "view" -> optional(nonEmptyText),
          "availableOnly" -> optional(boolean)
        )
      )

      val maybeSelectedVertical =  verticalStore.verticals.filter(v => v.urlSlug == vertical).headOption
      val (queryOption, sortOption, viewOption, availableOnlyOption) = marketplaceResultPageForm.bindFromRequest.get
      val maybeSortType = sortOption.flatMap(sort => CelebritySortingTypes(sort))
      val availableOnly = availableOnlyOption.getOrElse(false)

      val categoryAndCategoryValues = for {
        (key, set) <- request.queryString
        categoryRegex(id) <- categoryRegex findFirstIn key
      } yield {
        val categoryValueId = set.map(arg =>
          try {
            arg.toLong
          } catch {
            case e => throw new Exception("Invalid category value argument passed.", e)
          })
        (id.toLong, categoryValueId)
      }

      val categoryValuesRefinements = for ((category, categoryValues) <- categoryAndCategoryValues) yield categoryValues

      val verticalAndCategoryValues = maybeSelectedVertical match {
        case Some(vertical) => categoryValuesRefinements ++ List(Seq(vertical.categoryValue.id))
        case None => categoryValuesRefinements
      }

      val activeCategoryValues = {
        for {
          (category, categoryValues) <- categoryAndCategoryValues
          categoryValue <- categoryValues
        } yield { categoryValue }
      }.toSet

      val verticalViewModels = verticalStore.verticals.map { v =>
        val categories = for {
          category <- v.categoryValue.categories
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

      // Check if any search options have been defined
      if(queryOption.isDefined || !verticalAndCategoryValues.isEmpty) {
        val unsortedCelebrities = dbSession.connected(TransactionSerializable) {
          celebrityStore.marketplaceSearch(queryOption, verticalAndCategoryValues)
        }

        // Sort results
        val sortedCelebrities = maybeSortType.getOrElse(CelebritySortingTypes.MostRelevant) match {
          case MostRelevant => unsortedCelebrities
          case PriceAscending => unsortedCelebrities.toList.sortWith((a,b) => a.minPrice < b.minPrice)
          case PriceDescending => unsortedCelebrities.toList.sortWith((a,b) => a.maxPrice < b.maxPrice)
          case Alphabetical => unsortedCelebrities.toList.sortWith((a,b) => a.publicName < b.publicName)
          case _ => unsortedCelebrities
        }

        val celebrities = if(availableOnly){
          sortedCelebrities.filter(c => !c.soldout)
        } else {
          sortedCelebrities
        }

        val subtitle = queryOption match {
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
        val resultSets = for(vertical <- verticalStore.verticals) yield {
          val categoryValue = vertical.categoryValue
          val results = celebrityStore.marketplaceSearch(queryOption, List(Seq(categoryValue.id)))
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
