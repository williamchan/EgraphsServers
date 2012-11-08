package controllers

import play.api._
import play.api.mvc._
import models.frontend.storefront.{PersonalizeMessageOption, StorefrontOrderSummary, PersonalizeForm}
import java.util
import models.frontend.forms.FormError
import helpers.DefaultImplicitTemplateParameters

/**
 * Permutations of the Checkout: Personalize.
 */
object Personalize extends Controller with DefaultImplicitTemplateParameters {

  def index = Action {
    Ok(render())
  }

  def portrait = Action {
    Ok(render(
      productPreviewUrl = "http://placehold.it/302x420",
      orientation="orientation-portrait"
    ))
  }

  def allErrors = Action {
    Ok(render(personalizeForm=allErrorsPersonalizeForm))
  }

  def lowCharacterLimit = Action {
    Ok(render(writtenMessageCharacterLimit = 60))
  }

  def allPrePopulatedValues = Action {
    val default = defaultPersonalizeForm
    import default._

    Ok(render(personalizeForm=default.copy(
      isGift=isGift.copy(values=Some(true)),
      recipientName=recipientName.copy(values=Some("Erem Boto")),
      recipientEmail=recipientEmail.copy(values=Some("erem@egraphs.com")),
      messageOption=messageOption.copy(values=Some(PersonalizeMessageOption.CelebrityChoosesMessage)),
      messageText=messageText.copy(values=Some("Happy 30th birthday")),
      noteToCelebrity=noteToCelebrity.copy(values=Some("You're the best!"))
    )))
  }

  private def defaultPersonalizeForm = {
    PersonalizeForm.empty(
      "/POST-personalize",
      "isGiftParam",
      "recipientNameParam",
      "recipientEmailParam",
      "messageOptionsParam",
      "messageTextParam",
      "noteToCelebrityParam",
      "couponParam"
    )
  }

  private def defaultOrderSummary = {
    import frontend.formatting.MoneyFormatting.Conversions._

    StorefrontOrderSummary(
      celebrityName="Herp Derpson",
      productName="2012 MLG Finals",
      subtotal=BigDecimal(100.00).toMoney(),
      shipping=None,
      tax=None,
      total=BigDecimal(100.00).toMoney()
    )
  }

  private def allErrorsPersonalizeForm = {
    val default = defaultPersonalizeForm
    val error = Some(FormError("Get outa here this is no good!"))

    default.copy(
      recipientName=default.recipientName.copy(error=error),
      recipientEmail=default.recipientEmail.copy(error=error),
      messageText=default.messageText.copy(error=error),
      noteToCelebrity=default.noteToCelebrity.copy(error=error)
    )
  }

  private def render(
    personalizeForm: PersonalizeForm=defaultPersonalizeForm,
    guaranteedDelivery:util.Date = new util.Date(),
    writtenMessageCharacterLimit: Int = 60,
    orderSummary: StorefrontOrderSummary = defaultOrderSummary,
    productPreviewUrl: String = "http://placehold.it/454x288",
    orientation: String = "orientation-landscape"
  ) = {
    views.html.frontend.celebrity_storefront_personalize(
      form=personalizeForm,
      guaranteedDelivery=guaranteedDelivery,
      writtenMessageCharacterLimit=writtenMessageCharacterLimit,
      messageToCelebrityCharacterLimit=140,
      orderSummary=orderSummary,
      productPreviewUrl = productPreviewUrl,
      orientation=orientation
    )
  }
}
