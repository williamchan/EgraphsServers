package models.frontend.landing

/**
 *  A "Featured Star" as seen on the landing page
 **/
case class FeaturedStar(
  name: String,
  secondaryText: Option[String],
  imageUrl: String,
  storefrontUrl: String
)
