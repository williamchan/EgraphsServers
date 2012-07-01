package models.frontend.storefront

import models.frontend.forms.{FormError, Field}

case class PersonalizeForm (
  actionUrl: String,
  isGift: Field[Boolean],
  recipientName: Field[String],
  recipientEmail: Field[String],
  messageOption: Field[PersonalizeMessageOption],
  messageText: Field[String],
  noteToCelebrity: Field[String]
)

object PersonalizeForm {
  def empty(
    actionUrl: String,
    isGiftParam: String,
    recipientNameParam: String,
    recipientEmailParam: String,
    messageOptionParam: String,
    messageTextParam: String,
    noteToCelebrityParam: String
  ): PersonalizeForm =
  {
    import PersonalizeMessageOption.SpecificMessage
    PersonalizeForm(
      actionUrl=actionUrl,
      isGift = Field(isGiftParam, Some(false)),
      recipientName = Field(recipientNameParam, None),
      recipientEmail= Field(recipientEmailParam, None),
      messageOption=Field(messageOptionParam, Some(SpecificMessage)),
      messageText=Field(messageTextParam, None),
      noteToCelebrity=Field(noteToCelebrityParam, None)
    )
  }
}