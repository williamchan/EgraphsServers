package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import models.frontend.contents.Section
import models.frontend.marketplace._
import helpers.DefaultImplicitTemplateParameters
import scala.util.Random
import models.frontend.marketplace.CelebritySortingTypes

/**
 * Marketplace controller
 */
object Marketplace extends Controller with DefaultImplicitTemplateParameters {
  val coinflip = Random

  def results() = Action {
    Ok(views.html.frontend.marketplace_results(
      "",
      false,
      controllers.routes.Marketplace.results.url,
      verticalSet,
      resultSet,
      sortOptions,
      availableOnly = false))
  }

  def results_list() = Action {
    Ok(views.html.frontend.marketplace_results(
      "",
      true,
      controllers.routes.Marketplace.results.url,
      verticalSet,
      resultSet,
      sortOptions,
      availableOnly = coinflip.nextBoolean))
  }

  def categorySet: Iterable[CategoryViewModel] = {
    val categoryNames = List("Team", "Position").zipWithIndex
    val categoryValueNames = List("Category1", "Category2", "Category3", "Category4").zipWithIndex

    for ((cname: String, id: Int) <- categoryNames) yield {
      CategoryViewModel(
        id = id,
        publicName = cname,
        for ((cvname: String, id: Int) <- categoryValueNames) yield {
          CategoryValueViewModel(id = id, publicName = cvname, active = coinflip.nextBoolean)
        })
    }
  }

  def verticalSet: List[VerticalViewModel] = {
    List(
      VerticalViewModel(id = 1, verticalName = "mlb", publicName = "Major League Baseball", shortName = "MLB", iconUrl = "images/icon-logo-mlb.png", active = true, categoryViewModels = mlbCategories),
      VerticalViewModel(id = 2, verticalName = "nba", publicName = "National Basketball Association", shortName = "NBA", iconUrl = "images/icon-logo-nba.png", categoryViewModels = nbaCategories)
    )
  }

  def resultSet: ResultSetViewModel = {
      ResultSetViewModel(subtitle = Option("Derpson"), celebrities = celebViewModels(36))
  }

  def celebViewModels(quantity: Int): Iterable[MarketplaceCelebrity] = {
    for (i <- 0.until(quantity)) yield {
      MarketplaceCelebrity(
        id = i,
        publicName = "Herp Derpson",
        photoUrl = EgraphsAssets.at("images/660x350.gif").url,
        storefrontUrl = "#",
        inventoryRemaining = coinflip.nextInt(20),
        minPrice = 45,
        maxPrice = 90,
        secondaryText = "Boston Red Sox")
    }
  }

  val mlbCategories: List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 1,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(id = 2, publicName = "Boston Red Sox", active = true),
          CategoryValueViewModel(id = 3, publicName = "Miami Marlins", active = false),
          CategoryValueViewModel(id = 4, publicName = "New York Yankees", active = false),
          CategoryValueViewModel(id = 5, publicName = "Tampa Bay Rays", active = true),
          CategoryValueViewModel(id = 6, publicName = "Oakland Athletics", active = false),
          CategoryValueViewModel(id = 7, publicName = "San Francisco Giants", active = false)
        )
      ),
      CategoryViewModel(
        id = 2,
        publicName = "Position",
        categoryValues = List(
          CategoryValueViewModel(id = 5, publicName = "Pitcher", active = false),
          CategoryValueViewModel(id = 6, publicName = "Shortstop", active = true),
          CategoryValueViewModel(id = 7, publicName = "Catcher", active = false),
          CategoryValueViewModel(id = 8, publicName = "1st Baseman", active = false),
          CategoryValueViewModel(id = 9, publicName = "2nd Baseman", active = true),
          CategoryValueViewModel(id = 10, publicName = "3rd Baseman", active = false)
        )
      )
    )
  }

  val nbaCategories: List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 3,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(id = 2, publicName = "Boston Celtics", active = true),
          CategoryValueViewModel(id = 3, publicName = "Miami Heat", active = false),
          CategoryValueViewModel(id = 4, publicName = "San Antonio Spurs", active = false),
          CategoryValueViewModel(id = 5, publicName = "Los Angeles Clippers", active = true),
          CategoryValueViewModel(id = 6, publicName = "Golden State Warriors", active = false),
          CategoryValueViewModel(id = 7, publicName = "New York Knicks", active = false)
        )
      ),
      CategoryViewModel(
        id = 4,
        publicName = "Position",
        categoryValues = List(
          CategoryValueViewModel(id = 5, publicName = "Center", active = false),
          CategoryValueViewModel(id = 6, publicName = "Point Guard", active = true),
          CategoryValueViewModel(id = 7, publicName = "Power Forward", active = false),
          CategoryValueViewModel(id = 8, publicName = "Small Forward", active = false),
          CategoryValueViewModel(id = 9, publicName = "Shooting Guard", active = true)
        )
      )
    )
  }

  def sortOptions: Iterable[SortOptionViewModel] = {
    for {
      sortingType <- CelebritySortingTypes.values
    } yield {
      SortOptionViewModel(
        name = sortingType.name,
        display = sortingType.displayName,
        active = (sortingType == CelebritySortingTypes.PriceAscending)
      )
    }
  }
}

