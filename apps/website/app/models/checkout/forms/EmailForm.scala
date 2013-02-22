package models.checkout.forms

import play.api.data.{Form, Forms}


/** used for posting buyer and recipient emails from checkout page */
object EmailForm extends CheckoutForm[String] {

  object FormKeys {
    val emailKey = "email"
  }

  override def form = Form[String] {
    Forms.single(
      FormKeys.emailKey -> ApiForms.email
    )
  }
}
