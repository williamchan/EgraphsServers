package models.frontend.storefront

import org.joda.money.Money

/**
 * Face for a product as viewed as a tile in the Choose Photo page
 *
 * @param name the product's name or title, e.g. "NBA All Stars 2012"
 * @param price the product's price
 * @param imageUrl URL to the product photo at 340x200 px
 * @param targetUrl Link to select this image and move to its Carousel view.
 * @param orientation portrait or landscape
 */
case class ChoosePhotoTileProduct(
  name: String,
  price: Money,
  imageUrl: String,
  targetUrl: String,
  orientation: ProductOrientation
)
