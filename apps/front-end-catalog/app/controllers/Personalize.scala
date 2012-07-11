package controllers

import play.mvc.Controller
import models.frontend.storefront.{PersonalizeMessageOption, StorefrontOrderSummary, PersonalizeForm}
import java.util
import models.frontend.forms.FormError


/**
 * Permutations of the Checkout: Personalize.
 */
object Personalize extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{

  def index = {
    render()
  }

  def portrait = {
    render(orientation="orientation-portrait")
  }

  def allErrors = {
    render(personalizeForm=allErrorsPersonalizeForm)
  }

  def lowCharacterLimit = {
    render(writtenMessageCharacterLimit = 60)
  }

  def allPrePopulatedValues = {
    val default = defaultPersonalizeForm
    import default._

    render(personalizeForm=default.copy(
      isGift=isGift.copy(values=Some(true)),
      recipientName=recipientName.copy(values=Some("Erem Boto")),
      recipientEmail=recipientEmail.copy(values=Some("erem@egraphs.com")),
      messageOption=messageOption.copy(values=Some(PersonalizeMessageOption.CelebrityChoosesMessage)),
      messageText=messageText.copy(values=Some("Happy 30th birthday")),
      noteToCelebrity=noteToCelebrity.copy(values=Some("You're the best!"))
    ))
  }

  private def defaultPersonalizeForm = {
    PersonalizeForm.empty(
      "/POST-personalize",
      "isGiftParam",
      "recipientNameParam",
      "recipientEmailParam",
      "messageOptionsParam",
      "messageTextParam",
      "noteToCelebrityParam"
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
    writtenMessageCharacterLimit: Int = 100,
    orderSummary: StorefrontOrderSummary = defaultOrderSummary,
    orientation: String = "orientation-landscape"
  ) = {
    views.frontend.html.celebrity_storefront_personalize(
      form=personalizeForm,
      guaranteedDelivery=guaranteedDelivery,
      writtenMessageCharacterLimit=writtenMessageCharacterLimit,
      messageToCelebrityCharacterLimit=140,
      orderSummary=orderSummary,
      orientation=orientation
    )
  }
}
