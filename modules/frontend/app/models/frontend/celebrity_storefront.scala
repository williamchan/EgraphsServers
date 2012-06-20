package models.frontend.storefront

import java.util.Date
import org.joda.money.Money

sealed case class ProductOrientation(
  name: String,
  tileClass: String
)

case object PortraitOrientation extends ProductOrientation(
  name="portrait size",
  tileClass="orientation portrait"
)

case object LandscapeOrientation extends ProductOrientation(
  name="landscape size",
  tileClass="orientation landscape"
)

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
