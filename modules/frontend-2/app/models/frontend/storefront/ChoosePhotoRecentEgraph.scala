package models.frontend.storefront

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
