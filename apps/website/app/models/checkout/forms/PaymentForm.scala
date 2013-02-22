package models.checkout.forms

import models.checkout.CashTransactionLineItemType
import play.api.data.{Form, Forms}


object PaymentForm extends CheckoutForm[CashTransactionLineItemType] {

  object FormKeys {
    val stripeToken = "stripeToken"
    val postalCode = "postalCode"
  }

  override val form = Form[CashTransactionLineItemType] {
    import ApiForms._
    import FormKeys._

    Forms.mapping(
      stripeToken -> Forms.optional(text),
      postalCode -> Forms.optional(text(5,10))
    )(CashTransactionLineItemType.create)(unapplyToForm)
  }

  /** since `CashTransactionLineItemType` has other arguments, its default unapply doesn't work for the form */
  protected def unapplyToForm(txnType: CashTransactionLineItemType) = {
    Some(txnType.stripeCardTokenId, txnType.billingPostalCode)
  }
}