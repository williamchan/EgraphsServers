package models.frontend.storefront

sealed trait PersonalizeMessageOption

object PersonalizeMessageOption {
  case object SpecificMessage extends PersonalizeMessageOption
  case object AnythingHeWants extends PersonalizeMessageOption
  case object SignatureOnly extends PersonalizeMessageOption
}