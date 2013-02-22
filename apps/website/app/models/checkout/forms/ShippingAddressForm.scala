package models.checkout.forms

import models.{AddressServices, Address}
import play.api.data.{Forms, Form}
import services.{Time, AppConfig}


case class ShippingAddress(
  name: String,
  addressLine1: String,
  addressLine2: Option[String],
  city: String,
  state: String,
  postalCode: String
) {
  def stringify = listified mkString "\n"

  //
  // Helpers
  //
  def address = Address( // TODO: get rid of Address
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    _state = state,
    postalCode = postalCode
  )

  private def cityStateAndZip = "%s, %s %s".format(city, state, postalCode)
  private def listified = List(name, addressLine1) ++ addressLine2.toList ++ List(cityStateAndZip)
}


object ShippingAddressForm extends CheckoutForm[ShippingAddress] {

  object FormKeys {
    val nameKey = "name"
    val addressLine1Key = "addressLine1"
    val addressLine2Key = "addressLine2"
    val cityKey = "city"
    val stateKey = "state"
    val postalCodeKey = "postalCode"
  }

  override def form = Form[ShippingAddress] {
    import Forms.{ignored, mapping, optional}
    import ApiForms._
    import FormKeys._

    mapping ( // TODO(CE-13): set length on these if necessary
      nameKey -> nonEmpty(text),
      addressLine1Key -> nonEmpty(text),
      addressLine2Key -> optional(text),
      cityKey -> nonEmpty(text),
      stateKey -> nonEmpty(text),
      postalCodeKey -> nonEmpty(text)
    )(ShippingAddress.apply)(ShippingAddress.unapply)
  }
}
