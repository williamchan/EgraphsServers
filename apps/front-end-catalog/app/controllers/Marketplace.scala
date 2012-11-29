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
      query = "exampleQuery",
      viewAsList = false,
      marketplaceRoute = controllers.routes.Marketplace.results.url,
      verticalSet,
      resultSet,
      categoryViewModels,
      sortOptions))
  }

  def results_list() = Action {
    Ok(views.html.frontend.marketplace_results(
      query = "exampleQuery",
      viewAsList = true,
      marketplaceRoute = controllers.routes.Marketplace.mlb.url,
      verticalSet,
      resultSet,
      categoryViewModels,
      sortOptions))
  }

  def index() = Action {
    Ok(views.html.frontend.marketplace_landing("derp", "exampleQuery", verticalSet, resultSet, categoryViewModels, sortOptions))
  }

  def mlb() = Action {
    Ok(views.html.frontend.marketplace_mlb())
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

  def verticalSet: Iterable[VerticalViewModel] = {
    List(
      VerticalViewModel(id = 1, verticalName = "mlb", publicName = "Major League Baseball", shortName = "MLB", iconUrl = "images/icon-logo-mlb.png", active = true),
      VerticalViewModel(id = 2, verticalName = "nba", publicName = "National Basketball Association", shortName = "NBA", iconUrl = "images/icon-logo-nba.png"),
      VerticalViewModel(id = 3, verticalName = "mj", publicName = "Monster Jam", shortName = "Monster Jam", iconUrl = "images/icon-logo-monster-jam.png")).toSet
  }

  def resultSet: Iterable[ResultSetViewModel] = {
    List(ResultSetViewModel(subtitle = Option("Herpson"), celebrities = celebViewModels(3)),
      ResultSetViewModel(subtitle = Option("Derpson"), celebrities = celebViewModels(5)),
      ResultSetViewModel(subtitle = Option("Perpson"), celebrities = celebViewModels(9)))
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

  def categoryViewModels: Iterable[CategoryViewModel] = {
    List(
      CategoryViewModel(
        id = 1,
        publicName = "Team",
        categoryValues = List(
          CategoryValueViewModel(id = 2, publicName = "Boston Red Sox", active = true),
          CategoryValueViewModel(id = 3, publicName = "Miami Marlins", active = false),
          CategoryValueViewModel(id = 4, publicName = "New York Yankees", active = false)
        )
      ),
      CategoryViewModel(
        id = 2,
        publicName = "Position",
        categoryValues = List(
          CategoryValueViewModel(id = 5, publicName = "Pitcher", active = false),
          CategoryValueViewModel(id = 6, publicName = "Shortstop", active = true),
          CategoryValueViewModel(id = 7, publicName = "Catcher", active = false)
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

