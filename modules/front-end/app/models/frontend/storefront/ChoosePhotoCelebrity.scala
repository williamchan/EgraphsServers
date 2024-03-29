package models.frontend.storefront

import java.util

/**
 * Face for a Celebrity as viewed in the Choose Photo page
 *
 *
 * @param name the celebrity's name. e.g. "David Ortiz"
 * @param profileUrl URL for the celebrity's profile image
 * @param organization e.g. "Major League Baseball"
 * @param roleDescription e.g. "Pitcher, Tampa Bay Rays"
 * @param bio Detailed description of the celebrity.
 * @param twitterUsername The celebrity's username on twitter
 */
case class ChoosePhotoCelebrity(
  name: String,
  profileUrl: String,
  organization: String,
  roleDescription: String,
  bio: String,
  twitterUsername: Option[String]
)
