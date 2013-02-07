package models.checkout.data

import models.checkout.CashTransactionLineItemType
import play.api.data.{Form, Forms}



object PaymentForm extends CheckoutForm[CashTransactionLineItemType] {

  object FormKeys {
    val stripeToken = "stripeToken"
    val postalCode = "postalCode"
  }


  override val form = Form[CashTransactionLineItemType] {
    import Forms._
    import FormKeys._

    mapping(
      stripeToken -> optional(text),
      postalCode -> optional(text(5,10))
    )(applyToForm)(unapplyToForm)
  }

  protected override val formErrorByField = Map(FormKeys.postalCode -> ApiFormError.InvalidLength)

  protected def applyToForm(stripeToken: Option[String], postalCode: Option[String]) = {
    CashTransactionLineItemType(postalCode, stripeToken)
  }

  protected def unapplyToForm(txnType: CashTransactionLineItemType) = {
    Some(txnType.stripeCardTokenId, txnType.billingPostalCode)
  }
}
