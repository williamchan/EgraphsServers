package models.frontend.storefront

import java.util.Date
import org.joda.money.Money

sealed case class ProductOrientation(tileClass: String)

case object PortraitOrientation extends ProductOrientation(tileClass="orientation portrait")
case object LandscapeOrientation extends ProductOrientation(tileClass="orientation landscape")

case class ChoosePhotoCelebrity(
  name: String,
  profileUrl: String,
  category: String,
  categoryRole: String,
  twitterUsername: String,
  quantityAvailable: Int,
  deliveryDate: Date
)

case class ChoosePhotoProductTile(
  name: String,
  price: Money,
  imageUrl: String,
  targetUrl: String,
  orientation: ProductOrientation
)
