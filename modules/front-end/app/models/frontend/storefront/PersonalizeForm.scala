package models.frontend.storefront

import models.frontend.forms.Field

/**
 * View model for the personalize form, as rendered here:
 * [[views.html.frontend.celebrity_storefront_personalize]]
 *
 *
 * @param actionUrl target for POSTing the form
 * @param isGift true that the egraph is a gift
 * @param recipientName the name of the recipient
 * @param recipientEmail the recipient's email address
 * @param messageOption what the celebrity should write: a specific message,
 *    whatever he wants, or just his signature.
 * @param messageText text for the message.
 * @param noteToCelebrity a letter for the celebrity's eyes only.
 */
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
  /**
   * Returns an empty, basic version of the form
   * with parameter names as specified in these arguments.
   */
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