package models.frontend.landing

 /**
 * A "Catalog Star" as seen in the celebrity catalog and in the Featured Stars on the
 * home page
 *
 * @param name the star's name, e.g. "David Ortiz"
 * @param secondaryText pithy explanatory text, e.g. "Pitcher, Boston Red Sox"
 * @param imageUrl URL to the thumbnail image.
 * @param storefrontUrl URL to the celebrity's storefront.
 * @param hasInventoryRemaining true that the celebrity still has egraphs available to buy
 * @param isFeatured true that the celebrity should appear under "Featured Stars"
 */
case class CatalogStar(
  name: String,
  secondaryText: Option[String],
  imageUrl: String,
  storefrontUrl: String,
  hasInventoryRemaining: Boolean,
  isFeatured: Boolean
)
