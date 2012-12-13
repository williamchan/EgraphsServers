package models.categories

import com.google.inject.Inject

object VerticalStore {
  val categoryName = "Vertical"
}

case class Vertical(categoryValue: CategoryValue, shortName: String, urlSlug: String, iconUrl: String, featuredQuery: String)

class VerticalStore @Inject() (categoryStore: CategoryStore) {

  lazy val verticals : Iterable[Vertical] = {
    for(cv <- category.categoryValues) yield {
      cv.name match {
        case "MLB" => Vertical(categoryValue = cv, shortName = "MLB", urlSlug = "major-league-baseball", iconUrl = "images/icon-logo-mlb.png", featuredQuery ="mlb-featured")
        case "NBA" => Vertical(categoryValue = cv, shortName = "NBA", urlSlug = "national-basketball-association", iconUrl = "images/icon-logo-nba.png", featuredQuery="nba-featured")
      }
    }
  }

  // this has a side effect of creating the category if it isn't there
  lazy val category: Category = {
    categoryStore.findByName(VerticalStore.categoryName).getOrElse {
      Category(
        name = VerticalStore.categoryName,
        publicName = VerticalStore.categoryName).save()
    }
  }
}



