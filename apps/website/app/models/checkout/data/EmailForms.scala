package models.checkout.data

import play.api.data.{Form, Forms}



case class CustomerEmail(email: String)


object EmailForms extends CheckoutForm[CustomerEmail] {

  object FormKeys {
    val emailKey = "email"
  }

  override val form: Form[CustomerEmail] = {
    import Forms._
    Form(
      mapping(
        FormKeys.emailKey -> email
      )(CustomerEmail.apply)(CustomerEmail.unapply)
    )
  }

  protected override val formErrorByField = {
    import ApiFormError._
    import FormKeys._

    Map(emailKey -> InvalidFormat)
  }
}
