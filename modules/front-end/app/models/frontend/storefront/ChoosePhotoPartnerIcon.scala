package models.frontend.storefront

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
