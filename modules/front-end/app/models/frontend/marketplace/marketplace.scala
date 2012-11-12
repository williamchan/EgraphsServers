package models.frontend.marketplace

import play.api.libs.json._


case class MarketplaceCelebrity(
    id: Long  = 0,
    publicName: String,
    photoUrl: String,
    storefrontUrl: String,
    soldout: Boolean,
    minPrice: Int,
    maxPrice: Int, 
    subtitle: String
  ) 

 case class CategoryViewModel(
  id: Long = 0,
  publicName: String,
  categoryValues: Seq[CategoryValueViewModel]
) {
  def asActiveMap : Map[String, JsValue] = {
      Map(
         "c" + id.toString -> 
          Json.toJson(categoryValues.filter(cv => cv.active).map( fv => 
            Json.toJson(fv.id)  
          ).toSeq)
      )
  }
  
  def active : Boolean = {
    categoryValues.exists(cv => cv.active == true)
  } 
}

case class CategoryValueViewModel(
  id: Long= 0,
  publicName: String, 
  active: Boolean
) 


case class VerticalViewModel(
  verticalName: String, 
  publicName: String,
  shortName: String,
  iconUrl: String,
  active: Boolean = false
)

case class ResultSetViewModel(
    subtitle: Option[String],
    celebrities: Iterable[MarketplaceCelebrity] = List()
)
