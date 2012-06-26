package models.frontend.storefront

import java.util.Date
import org.joda.money.Money

/**
 * Encapsulates Portrait vs Landscape orientation in the storefront.
 * @param name the name of the orientation
 * @param tileClass the class that should be assigned in the choose photo page
 *    for the given orientation.
 */
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


/**
 * Face for a Celebrity as viewed in the Choose Photo page
 *
 * @param name the celebrity's name. e.g. "David Ortiz"
 * @param profileUrl URL for the celebrity's profile image
 * @param category e.g. "Major League Baseball"
 * @param categoryRole e.g. "Pitcher, Tampa Bay Rays"
 * @param bio Detailed description of the celebrity.
 * @param twitterUsername The celebrity's username on twitter
 * @param quantityAvailable The quantity of egraphs the celebrity is still signing
 * @param deliveryDate The guaranteed delivery date for any egraph you purchase from the
 *   celebrity.
 */
case class ChoosePhotoCelebrity(
  name: String,
  profileUrl: String,
  category: String,
  categoryRole: String,
  bio:String,
  twitterUsername: String,
  quantityAvailable: Int,
  deliveryDate: Date
)

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

/**
 * Face for a recently published Egraph as previewed on the ChoosePhoto pages.
 *
 * @param productTitle title of the photo that was signed
 * @param ownersName name of the person that owns the egraph
 * @param imageUrl URL to the egraph's thumbnail photo at 340x200 px
 * @param url URL to the egraph itself.
 */
case class ChoosePhotoRecentEgraph(
  productTitle: String,
  ownersName: String,
  imageUrl: String,
  url: String
)

/**
 * Icon for partner organization as presented on the ChoosePhoto pages.
 *
 * @param partnerName the partner's name, e.g. "MLB.com"
 * @param imageUrl URL to the image at 340x200 px
 * @param link URL to the partner organization's website.
 */
case class ChoosePhotoPartnerIcon(
  partnerName: String,
  imageUrl: String,
  link: String
)


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
  carouselUrl:String,
  facebookShareLink: String,
  twitterShareLink: String
)