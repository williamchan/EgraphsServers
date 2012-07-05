package models.frontend.storefront

case class FinalizePersonalizationViewModel (
  celebName: String,
  productTitle: String,
  recipientName: String,
  messageOption: PersonalizeMessageOption,
  messageText: String,
  editUrl: String
)
