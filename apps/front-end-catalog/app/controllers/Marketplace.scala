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

  def landing() = Action {
    Ok(views.html.frontend.marketplace_landing(
      controllers.routes.Marketplace.results.url,
      landingVerticalSet,
      landingResults)
    )
  }

  def verticalSet: List[VerticalViewModel] = {
    List(
      VerticalViewModel(id = 1, urlSlug="/major-league-baseball",verticalName = "mlb", publicName = "Major League Baseball", shortName = "MLB", iconUrl = "images/icon-logo-mlb.png", active = true, categoryViewModels = mlbCategories()),
      VerticalViewModel(id = 2, urlSlug="/national-basketball-association", verticalName = "nba", publicName = "National Basketball Association", shortName = "NBA", iconUrl = "images/icon-logo-nba.png", categoryViewModels = nbaCategories() )
    )
  }

  def landingVerticalSet: List[VerticalViewModel] = {
    List(
      VerticalViewModel(id = 1, urlSlug="/major-league-baseball",verticalName = "mlb", publicName = "Major League Baseball", shortName = "MLB", iconUrl = "images/icon-logo-mlb.png", categoryViewModels = mlbCategories(Option(false))),
      VerticalViewModel(id = 2, urlSlug="/national-basketball-association", verticalName = "nba", publicName = "National Basketball Association", shortName = "NBA", iconUrl = "images/icon-logo-nba.png", categoryViewModels = nbaCategories(Option(false)))
    )
  }

  def resultSet: ResultSetViewModel = {
      ResultSetViewModel(subtitle = Option("Showing 36 Results"), celebrities = celebViewModels(36))
  }

  def landingResults: List[ResultSetViewModel] = {
    List(
      ResultSetViewModel(subtitle = Option("Major League Baseball"), verticalUrl = Option("/major-league-baseball"), celebrities = celebViewModels(3)),
      ResultSetViewModel(subtitle = Option("National Basketball Association"), verticalUrl = Option("/national-basketball-association"), celebrities = celebViewModels(3))
    )
  }


  def celebViewModels(quantity: Int): Iterable[MarketplaceCelebrity] = {
    for (i <- 0.until(quantity)) yield {
      MarketplaceCelebrity(
        id = i,
        publicName = "David Ortiz",
        photoUrl = EgraphsAssets.at("images/660x350.gif").url,
        storefrontUrl = "#",
        inventoryRemaining = coinflip.nextInt(20),
        minPrice = 45,
        maxPrice = 90,
        secondaryText = "Boston Red Sox")
    }
  }

  def mlbCategories(active: Option[Boolean] = None): List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 1,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(id = 2, publicName = "Boston Red Sox", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 3, publicName = "Miami Marlins", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 4, publicName = "New York Yankees", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 5, publicName = "Tampa Bay Rays", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 6, publicName = "Oakland Athletics", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 7, publicName = "San Francisco Giants", active = active.getOrElse(coinflip.nextBoolean))
        )
      ),
      CategoryViewModel(
        id = 2,
        publicName = "Position",
        categoryValues = List(
          CategoryValueViewModel(id = 5, publicName = "Pitcher", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 6, publicName = "Shortstop", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 7, publicName = "Catcher", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 8, publicName = "1st Baseman", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 9, publicName = "2nd Baseman", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 10, publicName = "3rd Baseman", active = active.getOrElse(coinflip.nextBoolean))
        )
      )
    )
  }

  def nbaCategories(active: Option[Boolean] = None): List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 3,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(id = 2, publicName = "Boston Celtics", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 3, publicName = "Miami Heat", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 4, publicName = "San Antonio Spurs", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 5, publicName = "Los Angeles Clippers", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 6, publicName = "Golden State Warriors", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 7, publicName = "New York Knicks", active = active.getOrElse(coinflip.nextBoolean))
        )
      ),
      CategoryViewModel(
        id = 4,
        publicName = "Position",
        categoryValues = List(
          CategoryValueViewModel(id = 5, publicName = "Center", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 6, publicName = "Point Guard", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 7, publicName = "Power Forward", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 8, publicName = "Small Forward", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 9, publicName = "Shooting Guard", active = active.getOrElse(coinflip.nextBoolean))
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

