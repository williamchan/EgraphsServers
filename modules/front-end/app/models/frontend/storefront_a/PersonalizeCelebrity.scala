package models.frontend.storefront_a

import models.frontend.PersonalPronouns
import org.joda.money.Money
import play.api.templates.{HtmlFormat, Html}
import play.api.libs.json._
import HtmlFormat.escape
import frontend.formatting.MoneyFormatting.Conversions._

case class PersonalizeStar (
  id: Long,
  name: String,
  products: Seq[PersonalizeProduct],
  pronoun: PersonalPronouns
)

case class PersonalizeProduct (
  id: Long,
  title: String,
  description: String,
  price: Money,
  selected: Boolean,
  smallThumbUrl: String,
  largeThumbUrl: String
) {
  def toJson: JsValue = {
    Json.obj(
      "id" -> id,
      "title" -> title,
      "description" -> description,
      "price" -> price.getAmount.doubleValue(),
      "currencySymbol" -> price.getCurrencyUnit.getSymbol,
      "selected" -> selected,
      "thumbnails" -> Json.obj(
        "small" -> smallThumbUrl,
        "large" -> largeThumbUrl
      )
    )
  }
}