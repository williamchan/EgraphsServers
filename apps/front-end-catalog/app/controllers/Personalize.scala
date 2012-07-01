package controllers

import play.mvc.Controller
import models.frontend.storefront.{StorefrontOrderSummary, PersonalizeForm}
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
    views.frontend.html.celebrity_storefront_personalize(
      form=defaultPersonalizeForm,
      guaranteedDelivery=new util.Date,
      orderSummary=defaultOrderSummary
    )
  }

  def allErrors = {
    views.frontend.html.celebrity_storefront_personalize(
      form=allErrorsPersonalizeForm,
      guaranteedDelivery=new util.Date,
      orderSummary=defaultOrderSummary
    )
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
}
