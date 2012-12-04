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
    inventoryRemaining: Int,
    minPrice: Int,
    maxPrice: Int, 
    secondaryText: String)
{
  def soldout: Boolean = (inventoryRemaining <= 0)
}

/**
 * Represents a collection of results from a query. 
 * Subtitle is usually something like "Showing 10 results for 'derp'"
 **/
case class ResultSetViewModel(
    subtitle: Option[String],
    celebrities: Iterable[MarketplaceCelebrity] = List()
)

object MarketplaceConversions {
  implicit object MarketplaceCelebrityFormat extends Format[MarketplaceCelebrity] {
    def reads(json: JsValue) = MarketplaceCelebrity (
      (json \ "id").as[Long],
      (json \ "publicName").as[String],
      (json \ "photoUrl").as[String],
      (json \ "storefrontUrl").as[String],
      (json \ "inventoryRemaining").as[Int],
      (json \ "minPrice").as[Int],
      (json \ "maxPrice").as[Int],
      (json \ "secondaryText").as[String]
    )

    def writes(c: MarketplaceCelebrity) : JsValue = JsObject(List(
      "id" -> JsNumber(c.id),
      "publicName" -> JsString(c.publicName),
      "photoUrl" -> JsString(c.photoUrl),
      "storefrontUrl" -> JsString(c.storefrontUrl),
      "inventoryRemaining" -> JsNumber(c.inventoryRemaining),
      "soldout" -> JsBoolean(c.soldout),
      "minPrice" -> JsNumber(c.minPrice),
      "maxPrice" -> JsNumber(c.maxPrice),
      "secondaryText" -> JsString(c.secondaryText)
    ))
  }

  implicit object ResultSetViewModelFormat extends Format[ResultSetViewModel] {
    def reads(json: JsValue) : ResultSetViewModel = { ResultSetViewModel(
      (json \ "subtitle").asOpt[String],
      (json \ "celebrities").as[List[MarketplaceCelebrity]]
    )}

    def writes(r: ResultSetViewModel) : JsValue =  JsObject(List(
      "subtitle" -> JsString(r.subtitle.getOrElse("")),
      "celebrities" -> JsArray(r.celebrities.map (c => Json.toJson(c)).toSeq)
    ))
  }
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
  publicName: String, 
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
  verticalName: String, 
  publicName: String,
  shortName: String,
  iconUrl: String,
  active: Boolean = false,
  id: Long
)

