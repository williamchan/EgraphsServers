package models.frontend.marketplace

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
 * The active state in these classes implies that the object is selected, or otherwise affecting the results 
 * of the data that is being rendered. This ties in with our front end code that uses the "active" class to signify elements
 * that are selected, toggled on, or otherwise activated to represent the data model. 
 **/

case class MarketplaceCelebrity(
    id: Long  = 0,
    publicName: String,
    photoUrl: String,
    storefrontUrl: String,
    inventoryRemaining: Int,
    minPrice: Int,
    maxPrice: Int, 
    secondaryText: String)
{
  def soldout: Boolean = (inventoryRemaining <= 0)
  def hasInventoryRemaining: Boolean = !soldout
}

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object MarketplaceCelebrity extends Function8[Long, String, String, String, Int, Int, Int, String, MarketplaceCelebrity]{
  implicit val marketplaceCelebrityFormats = Json.format[MarketplaceCelebrity]
}

/**
 * Represents a collection of results from a query. 
 * Subtitle is usually something like "Showing 10 results for 'derp'"
 **/
case class ResultSetViewModel(
    subtitle: Option[String],
    verticalUrl: Option[String] = None,
    celebrities: Iterable[MarketplaceCelebrity] = List()
)

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object ResultSetViewModel extends Function3[Option[String], Option[String], Iterable[MarketplaceCelebrity], ResultSetViewModel] {
  implicit val resultSetViewModelFormats = Json.format[ResultSetViewModel]
}

/**
 * Represents a category and the values that are user-selectable for filtering.
 * For ex: Baseball Team with values {Boston Red Sox, New York Yankees}
 **/
case class CategoryViewModel(
  id: Long = 0,
  publicName: String,
  categoryValues: List[CategoryValueViewModel]
) {
  /**
   * Turn this representation into a convenient JSON representation for our front end code. 
   **/
  def asActiveMap : Map[String, JsValue] = {
      Map(
         "c" + id.toString -> 
          Json.toJson(categoryValues.filter(cv => cv.active).map( fv => 
            Json.toJson(fv.id)  
          ))
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
case class CategoryValueViewModel (
  id: Long = 0,
  url: String = "",
  publicName: String,
  iconUrl: String = "",
  active: Boolean
) extends Ordered[CategoryValueViewModel] {
  override def compare(that: CategoryValueViewModel) = {
    this.publicName.compareTo(that.publicName)
  }
}

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
  altText: String = "",
  verticalName: String, 
  publicName: String,
  shortName: String,
  urlSlug: String,
  iconUrl: Option[String] = None,
  tileUrl: Option[String] = None,
  active: Boolean = false,
  id: Long,
  categoryViewModels : Iterable[CategoryViewModel]
)

