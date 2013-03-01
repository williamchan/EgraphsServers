package models.categories

import com.google.inject.Inject
import services.Utils

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
 * @param tileUrl Tile for display on HP, 600x368 (16:9)
 * @param featuredQuery a search term which will generate the top three results for a given vertical on the marketplace
 *                      landing page.
 */
case class Vertical(categoryValue: CategoryValue,
                    shortName: String,
                    urlSlug: String,
                    iconUrl: Option[String] = None,
                    tileUrl: Option[String] = None,
                    featuredQuery: Option[String] = None) {
  lazy val categories = {
    categoryValue.categories
  }

}

class VerticalStore @Inject() (categoryStore: CategoryStore) {

  lazy val verticals : Iterable[Vertical] = {
    val maybeVerticals = for(cv <- category.categoryValues) yield {
      cv.name match {
        case "MLB" => Some(Vertical(categoryValue = cv, shortName = "MLB", urlSlug = "major-league-baseball",
          iconUrl = Some("images/icon-logo-mlb.png"), tileUrl = Some("images/mlb-stadium.jpg"), featuredQuery =Some("mlb-featured")))
        case "NBA" => Some(Vertical(categoryValue = cv, tileUrl = Some("images/nba-stadium.jpg"), shortName = "NBA", urlSlug = "national-basketball-association",
          iconUrl = Some("images/icon-logo-nba.png"), featuredQuery =Some("nba-featured")))
        case "Racing" => Some(Vertical(categoryValue = cv, shortName = "Racing", urlSlug = "racing",
          featuredQuery =Some("racing-featured")))
        // If someone adds a vertical, throw it on the left hand side.
        case _ => Some(Vertical(categoryValue = cv, shortName = cv.publicName, urlSlug = Utils.slugify(cv.publicName, true), featuredQuery = None))
      }
    }

    for(maybeVertical <- maybeVerticals;
        vertical <- maybeVertical ) yield {
      vertical
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



