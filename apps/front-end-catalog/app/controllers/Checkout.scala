package controllers

import play.mvc.Controller
import models.frontend.storefront.{CheckoutOrderSummary, CheckoutShippingAddressFormView, CheckoutBillingInfoView, CheckoutFormView}
import org.joda.money.{CurrencyUnit, Money}
import models.frontend.forms.Field

/**
 * Permutations of the Checkout: Checkout.
 */
object Checkout extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{
  //
  // Public members
  //
  def index = {
    render()
  }

  def portrait = {
    render(
      productPreviewUrl = "http://placehold.it/302x420",
      orientation="orientation-portrait"
    )
  }

  def allErrors = {
    render(form=allErrorsCheckoutForm)
  }

  def noShipping = {
    render(form=defaultCheckoutForm.copy(shipping=None))
  }

  def stripePayment = {
    render(paymentJsModule = "stripe-payment")
  }

  //
  // Private members
  //
  private def defaultOrderSummary = {
    CheckoutOrderSummary(
      celebrityName="{celebrity name}",
      productName="{product name}",
      recipientName="{recipient name}",
      messageText="{message text}",
      basePrice=Money.zero(CurrencyUnit.USD),
      shipping=None,
      tax=None,
      total=Money.zero(CurrencyUnit.USD)
    )
  }

  private def defaultCheckoutForm = {
    CheckoutFormView.empty(
      actionUrl="this-is-the-form-action",
      billing=CheckoutBillingInfoView.empty(
        "fullNameParam",
        "emailParam",
        "zipParam"
      ),
      shipping=Some(defaultShippingForm)
    )
  }

  private def allErrorsCheckoutForm = {
    val error = "This is not OK."
    val default = defaultCheckoutForm
    val defaultBilling = default.billing

    val newBilling = defaultBilling.copy(
      fullName=defaultBilling.fullName.withError(error),
      email=defaultBilling.email.withError(error),
      postalCode=defaultBilling.postalCode.withError(error)
    )

    val newShipping = defaultShippingForm.copy(
      fullName=defaultShippingForm.fullName.withError(error),
      address1=defaultShippingForm.address1.withError(error),
      address2=defaultShippingForm.address2.withError(error),
      city=defaultShippingForm.city.withError(error),
      state=defaultShippingForm.state.withError(error),
      postalCode=defaultShippingForm.postalCode.withError(error)
    )

    defaultCheckoutForm.copy(
      billing=newBilling,
      shipping=Some(newShipping)
    )
  }

  private def defaultShippingForm = {
    CheckoutShippingAddressFormView.empty(
      "fullNameParam",
      "phoneParam",
      "address1Param",
      "address2Param",
      "cityParam",
      "stateParam",
      "postalCodeParam"
    )
  }

  val testStripeKey = {
    "pk_qIGUDirehUxj2GTFwgeRBkOfHIWdX"
  }

  private def render(
    form: CheckoutFormView=defaultCheckoutForm,
    summary: CheckoutOrderSummary=defaultOrderSummary,
    paymentJsModule: String="yes-maam-payment",
    paymentPublicKey: String=testStripeKey,
    productPreviewUrl: String = "http://placehold.it/454x288",
    orientation: String = "orientation-landscape"
  ) = {
    views.frontend.html.celebrity_storefront_checkout(
      form=form,
      summary=summary,
      paymentJsModule=paymentJsModule,
      paymentPublicKey=testStripeKey,
      productPreviewUrl=productPreviewUrl,
      orientation=orientation
    )
  }
}

