package models.frontend.storefront

import java.util

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
 *                     celebrity.
 */
case class ChoosePhotoCelebrity(
  name: String,
  profileUrl: String,
  category: String,
  categoryRole: String,
  bio: String,
  twitterUsername: String,
  quantityAvailable: Int,
  deliveryDate: util.Date
)
