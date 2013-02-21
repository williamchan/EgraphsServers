package models.frontend.storefront_a

import models.frontend.PersonalPronouns
import org.joda.money.Money
import play.api.templates.Html

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
  def toJson: Html = {
    Html(
      "{id:" + id + "," +
      " title:'" + title + "'," +
      " description:'" + description + "'," +
      " price:" + price.getAmount + "," +
      " currencySymbol:'" + price.getCurrencyUnit.getSymbol + "'," +
      " selected:" + selected + "," +
      " thumbnails: {" +
      "   small:'" + smallThumbUrl + "'," +
      "   large:'" + largeThumbUrl + "'" +
      " }" +
      "} "
    )
  }
}

