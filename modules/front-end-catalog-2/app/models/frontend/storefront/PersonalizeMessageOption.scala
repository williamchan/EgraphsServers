package models.frontend.storefront

/**
 * Choices for how the written message should be personalized.
 */
sealed abstract class PersonalizeMessageOption(val paramValue: String)

object PersonalizeMessageOption {
  case object SpecificMessage extends PersonalizeMessageOption("SignatureWithMessage")
  case object CelebrityChoosesMessage extends PersonalizeMessageOption("CelebrityChoosesMessage")
  case object SignatureOnly extends PersonalizeMessageOption("SignatureOnly")
}