package models.frontend.marketplace

import play.api.libs.json._

/**
 *  Classes for results page
 **/

/**
 * Represents a celebrity in the results view of the marketplace
 * MarketplaceCelebrity(
 *  id = 10,
 *  publicName = "Herp Derpson",
 *  photoUrl = "/images/public/rendered-at-660-width.jpg",
 *  storefrontUrl = "routes/to/celeb/page,
 *  soldout = true,
 *  minPrice =  100,
 *  maxPrice = 10000, 
 *  secondaryText = "Boston Red Sox"
 * ) 
 **/
case class MarketplaceCelebrity(
    id: Long  = 0,
    publicName: String,
    photoUrl: String,
    storefrontUrl: String,
    soldout: Boolean,
    minPrice: Int,
    maxPrice: Int, 
    secondaryText: String
  ) 

/**
 * Represents a category and the values that are user-selectable for filtering.
 * For ex: Baseball Team with values {Boston Red Sox, New York Yankees}
 **/
case class CategoryViewModel(
  id: Long = 0,
  publicName: String,
  categoryValues: Iterable[CategoryValueViewModel]
) {
  /**
   * Turn this representation into a convenient JSON representation for our front end code. 
   **/
  def asActiveMap : Map[String, JsValue] = {
      Map(
         "c" + id.toString -> 
          Json.toJson(categoryValues.filter(cv => cv.active).map( fv => 
            Json.toJson(fv.id)  
          ).toSeq)
      )
  }
  /**
   * Determine whether or not there is an active value in this code. 
   **/
  def active : Boolean = {
    categoryValues.exists(cv => cv.active == true)
  } 
}
 /**
  *  Represents a CategoryValue (for ex: Boston Red Sox)
  **/
case class CategoryValueViewModel(
  id: Long= 0,
  publicName: String, 
  active: Boolean
) 
/**
 * Representation of sorting options like Price ascending or Alphabetical
 **/
case class SortOptionViewModel(
  name: String,
  display: String,
  active: Boolean
)
/**
 * Currently unused
 * Representation of Verticals like MLB, NBA, etc.
 *
 **/ 
case class VerticalViewModel(
  verticalName: String, 
  publicName: String,
  shortName: String,
  iconUrl: String,
  active: Boolean = false,
  id: Long
)

/**
 * Represents a collection of results from a query. 
 * Subtitle is usually something like "Showing 10 results for 'derp'"
 **/
case class ResultSetViewModel(
    subtitle: Option[String],
    celebrities: Iterable[MarketplaceCelebrity] = List()
)