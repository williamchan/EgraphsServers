package models.frontend.storefront

sealed abstract class PersonalizeMessageOption(val paramValue: String)

object PersonalizeMessageOption {
  case object SpecificMessage extends PersonalizeMessageOption("SignatureWithMessage")
  case object AnythingHeWants extends PersonalizeMessageOption("CelebrityChoosesMessage")
  case object SignatureOnly extends PersonalizeMessageOption("SignatureOnly")
}