package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
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
      VerticalViewModel(id = 1, urlSlug="major-league-baseball",verticalName = "mlb", publicName = "Major League Baseball", shortName = "MLB", iconUrl = Some("images/icon-logo-mlb.png"), active = true, categoryViewModels = mlbCategories()),
      VerticalViewModel(id = 2, urlSlug="national-basketball-association", verticalName = "nba", publicName = "National Basketball Association", shortName = "NBA", iconUrl = Some("images/icon-logo-nba.png"), categoryViewModels = nbaCategories()),
      VerticalViewModel(id = 3, urlSlug="racing", verticalName = "racing", publicName = "Racing", shortName = "Racing", categoryViewModels = racingCategories() )
    )
  }

  def landingVerticalSet: List[VerticalViewModel] = {
    List(
      VerticalViewModel(id = 1,
        altText = "Get an egraph from an MLB star.",
        urlSlug="major-league-baseball",
        verticalName = "mlb",
        publicName = "Major League Baseball",
        shortName = "MLB",
        iconUrl = Some("images/icon-logo-mlb.png"),
        tileUrl = Some("images/mlb-stadium.jpg"),
        categoryViewModels = mlbCategories(Option(false))
      ),
      VerticalViewModel(id = 2,
        altText = "Get an egraph from an NBA star.",
        urlSlug="national-basketball-association",
        verticalName = "nba",
        publicName = "National Basketball Association",
        shortName = "NBA",
        iconUrl = Some("images/icon-logo-nba.png"),
        tileUrl = Some("images/nba-stadium.jpg"),
        categoryViewModels = mlbCategories(Option(false))
      ),
      VerticalViewModel(id = 3,
        altText = "Get an egraph from a racing star.",
        urlSlug="racing",
        verticalName = "racing",
        publicName = "Racing",
        shortName = "Racing",
        categoryViewModels = racingCategories()
      )
    )
  }

  private def resultSet: ResultSetViewModel = {
      ResultSetViewModel(subtitle = Option("Showing 36 Results"), celebrities = celebViewModels(36))
  }

  private def landingResults: List[ResultSetViewModel] = {
    List(
      ResultSetViewModel(subtitle = Option("Major League Baseball"), verticalUrl = Option("/major-league-baseball"), celebrities = celebViewModels(3)),
      ResultSetViewModel(subtitle = Option("National Basketball Association"), verticalUrl = Option("/national-basketball-association"), celebrities = celebViewModels(3)),
      ResultSetViewModel(subtitle = Option("Racing"), verticalUrl = Option("/racing"), celebrities = celebViewModels(2))
    )
  }


  private def celebViewModels(quantity: Int): Iterable[MarketplaceCelebrity] = {
    for (i <- 0.until(quantity)) yield {
      MarketplaceCelebrity(
        id = i,
        publicName = "David Ortiz",
        photoUrl = EgraphsAssets.at("images/660x350.gif").url,
        storefrontUrl = "#",
        inventoryRemaining = coinflip.nextInt(20),
        minPrice = 45,
        maxPrice = 90,
        secondaryText = "Los Angeles Angels of Anaheim")
    }
  }

  private def mlbCategories(active: Option[Boolean] = None): List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 1,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/ari.png", publicName = "Arizona Diamondbacks", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/atl.png", publicName ="Atlanta Braves", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/bal.png", publicName ="Baltimore Orioles", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/bos.png", publicName = "Boston Red Sox", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/chc.png", publicName ="Chicago Cubs", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/cws.png", publicName ="Chicago White Sox", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/cin.png", publicName ="Cincinnati Reds", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/cle.png", publicName ="Cleveland Indians", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/col.png", publicName ="Colorado Rockies", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/det.png", publicName ="Detroit Tigers", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/hou.png", publicName = "Houston Astros", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/kc.png",  publicName ="Kansas City Royals", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/laa.png", publicName ="Los Angeles Angels of Anaheim", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/lad.png", publicName ="Los Angeles Dodgers", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/mia.png", publicName = "Miami Marlins", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/mil.png", publicName ="Milwuakee Brewers", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/min.png", publicName ="Minnesota Twins", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/nym.png", publicName ="New York Mets", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/nyy.png", publicName ="New York Yankees", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/as.png",  publicName = "Oakland Athletics", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/phi.png", publicName = "Philadelphia Phillies", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/pit.png", publicName ="Pittsburgh Pirates", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/sd.png",  publicName ="San Diego Padres", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/sf.png",  publicName = "San Francisco Giants", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/sea.png", publicName = "Seattle Mariner", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/stl.png", publicName ="St. Louis Cardinals", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/tb.png",  publicName ="Tampa Bay Rays", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/tex.png", publicName ="Texas Rangers", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/tor.png", publicName ="Toronto Blue Jays", active = false),
          CategoryValueViewModel(url = "#", iconUrl = "images/icons/was.png", publicName ="Washington Nationals", active = false)
        )
      )
    )
  }

  private def nbaCategories(active: Option[Boolean] = None): List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 2,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(id = 2, publicName = "Boston Celtics", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 3, publicName = "Miami Heat", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 4, publicName = "San Antonio Spurs", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 5, publicName = "Los Angeles Clippers", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 6, publicName = "Golden State Warriors", active = active.getOrElse(coinflip.nextBoolean)),
          CategoryValueViewModel(id = 7, publicName = "New York Knicks", active = active.getOrElse(coinflip.nextBoolean))
        )
      )
    )
  }

  private def racingCategories(active: Option[Boolean] = None): List[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 3,
        publicName = "League",
        categoryValues = List(
          CategoryValueViewModel(id = 8, publicName = "Nascar", active = active.getOrElse(coinflip.nextBoolean))
        )
      )
    )
  }

  private def sortOptions: Iterable[SortOptionViewModel] = {
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

