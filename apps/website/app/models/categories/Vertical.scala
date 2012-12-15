package models.categories

import com.google.inject.Inject

/**
 * This is a hardcoded, limited version of how a true vertical store will work.
 * Since initialization happens with lazy vals, at application launch we expect
 * a Category named "Vertical", with two Category Values, one named "MLB" and one named "NBA"
 * This will NOT WORK if those values aren't in the database at application launch.
 */

object VerticalStore {
  val categoryName = "Vertical"
}

/**
 * Representation of a vertical. This is not encoded in any database.
 * @param categoryValue  The category value that corresponds to this vertical
 * @param shortName A short name, appropriate for the row of vertical buttons. Think acronyms.
 * @param urlSlug A urlSlug for our router. Think SEO & marketing.
 * @param iconUrl An icon for the vertical buttons. The size isn't standardized.
 * @param featuredQuery a search term which will generate the top three results for a given vertical on the marketplace
 *                      landing page.
 */
case class Vertical(categoryValue: CategoryValue,
                    shortName: String,
                    urlSlug: String,
                    iconUrl: String,
                    featuredQuery: String)

class VerticalStore @Inject() (categoryStore: CategoryStore) {

  lazy val verticals : Iterable[Vertical] = {
    for(cv <- category.categoryValues) yield {
      cv.name match {
        case "MLB" => Vertical(categoryValue = cv, shortName = "MLB", urlSlug = "major-league-baseball",
          iconUrl = "images/icon-logo-mlb.png", featuredQuery ="mlb-featured")
        case "NBA" => Vertical(categoryValue = cv, shortName = "NBA", urlSlug = "national-basketball-association",
          iconUrl = "images/icon-logo-nba.png", featuredQuery ="nba-featured")
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



