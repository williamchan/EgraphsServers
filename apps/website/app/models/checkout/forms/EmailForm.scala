package models.checkout.forms

import play.api.data.{Form, Forms}


/** used for posting buyer and recipient emails from checkout page */
case class BuyerDetails(name: Option[String], email: String)

object BuyerForm extends CheckoutForm[BuyerDetails] {
  object FormKeys {
    val emailKey = "email"
    val nameKey = "name"
  }

  override def form = Form[BuyerDetails] {
    import FormKeys._
    import Forms.optional
    import ApiForms._

    Forms.mapping (
      nameKey -> optional(text),
      emailKey -> email
    )(BuyerDetails.apply)(BuyerDetails.unapply)
  }
}

object RecipientForm extends CheckoutForm[String] {
  object FormKeys {
    val emailKey = "email"
  }

  override def form = Form[String] {
    Forms.single(
      FormKeys.emailKey -> ApiForms.email
    )
  }
}