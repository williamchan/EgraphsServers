package models.checkout.forms

import play.api.data.{Form, Forms}


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
