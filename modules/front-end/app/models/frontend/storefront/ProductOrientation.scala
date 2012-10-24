package models.frontend.storefront

/**
 * Encapsulates Portrait vs Landscape orientation in the storefront.
 * @param name the name of the orientation
 * @param tileClass the class that should be assigned in the choose photo page
 *                  for the given orientation.
 */
sealed case class ProductOrientation(
  name: String,
  tileClass: String
)


case object PortraitOrientation extends ProductOrientation(
  name = "portrait size",
  tileClass = "orientation portrait"
)


case object LandscapeOrientation extends ProductOrientation(
  name = "landscape size",
  tileClass = "orientation landscape"
)
