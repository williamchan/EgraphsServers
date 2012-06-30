package models.frontend.storefront

import java.util

/**
 * Face for a Celebrity as viewed in the Choose Photo page
 *
 * TODO: re-enable quantity available and delivery date
 *
 * @param name the celebrity's name. e.g. "David Ortiz"
 * @param profileUrl URL for the celebrity's profile image
 * @param category e.g. "Major League Baseball"
 * @param categoryRole e.g. "Pitcher, Tampa Bay Rays"
 * @param bio Detailed description of the celebrity.
 * @param twitterUsername The celebrity's username on twitter
 */
case class ChoosePhotoCelebrity(
  name: String,
  profileUrl: String,
  category: String,
  categoryRole: String,
  bio: String,
  twitterUsername: Option[String]
)
