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

  def index = {
    views.frontend.html.celebrity_storefront_checkout(
      form=defaultCheckoutForm,
      summary=defaultOrderSummary,
      paymentPublicKey=testStripeKey
    )
  }

  def allErrors = {
    views.frontend.html.celebrity_storefront_checkout(
      form=allErrorsCheckoutForm,
      summary=defaultOrderSummary,
      paymentPublicKey=testStripeKey
    )
  }

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
        "zipParam",
        "paymentTokenParam",
        "cardNumber",
        "cardExpiryMonth",
        "cardExpiryYear",
        "cardCvvParam"
      ),
      shipping=Some(defaultShippingForm),
      shippingIsSameAsBillingParam="shippingSameAsBilling"
    )
  }

  private def allErrorsCheckoutForm = {
    val error = "This is not OK."
    val default = defaultCheckoutForm
    val defaultBilling = default.billing

    val newBilling = defaultBilling.copy(
      fullName=defaultBilling.fullName.withError(error),
      email=defaultBilling.email.withError(error),
      postalCode=defaultBilling.postalCode.withError(error),
      paymentToken=defaultBilling.paymentToken.withError(error),
      cardNumber=defaultBilling.cardNumber.withError(error),
      cardExpiryMonth=defaultBilling.cardExpiryMonth.withError(error),
      cardExpiryYear=defaultBilling.cardExpiryYear.withError(error)
    )

    val newShipping = defaultShippingForm.copy(
      fullName=defaultShippingForm.fullName.withError(error),
      phone=defaultShippingForm.phone.withError(error),
      email=defaultShippingForm.phone.withError(error),
      address1=defaultShippingForm.address1.withError(error),
      address2=defaultShippingForm.address2.withError(error),
      city=defaultShippingForm.city.withError(error),
      state=defaultShippingForm.state.withError(error),
      postalCode=defaultShippingForm.postalCode.withError(error)
    )

    defaultCheckoutForm.copy(
      billing=newBilling,
      shipping=Some(newShipping),
      shippingIsSameAsBilling=Field("shipping-same-as-billing", Some(false))
    )
  }

  private def defaultShippingForm = {
    CheckoutShippingAddressFormView.empty(
      "fullNameParam",
      "phoneParam",
      "emailParam",
      "address1Param",
      "address2Param",
      "cityParam",
      "stateParam",
      "postalCodeParam"
    )
  }

  private val testStripeKey = {
    "pk_qIGUDirehUxj2GTFwgeRBkOfHIWdX"
  }
}

