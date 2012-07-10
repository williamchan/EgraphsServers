package models.frontend.storefront

import org.joda.money.Money

/**
 * Face for a Product as presented on the Choose Photo carousel view.
 * @param name the product's name, e.g. "NBA All Stars 2012
 * @param description the product's full
 * @param price the product's price
 * @param imageUrl link to the product's image at 400px height
 * @param personalizeLink link to the product's personalize page at
 * @param orientation the image's orientation
 * @param carouselUrl what the URL should change to when this product is selected on
 *     the carousel.
 * @param facebookShareLink link to share this photo on facebook
 * @param twitterShareLink link to share this photo on twitter
 */
case class ChoosePhotoCarouselProduct(
  name: String,
  description: String,
  price: Money,
  imageUrl: String,
  personalizeLink: String,
  orientation: ProductOrientation,
  carouselUrl: String,
  facebookShareLink: String,
  twitterShareLink: String,
  carouselViewLink: String,
  quantityRemaining: Int
)
