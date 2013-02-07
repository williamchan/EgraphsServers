package models.checkout.data

import play.api.data.{FormError, Form, Forms}
import models.{CouponServices, CouponStore, Coupon}
import services.AppConfig
import models.checkout.{CouponLineItemType, Checkout, LineItem, LineItemType}
import org.joda.money.{CurrencyUnit, Money}
import services.db.HasTransientServices










object CouponForm extends CheckoutForm[CouponLineItemType]{

  object FormKeys {
    val _codeLength = Coupon.defaultCodeLength
    val couponCode = "couponCode"
  }





  override val form = {
    import play.api.data.Forms._
    import FormKeys._

    Form[CouponLineItemType](
      mapping(
        couponCode -> nonEmptyText(_codeLength, _codeLength)
      )(CouponLineItemType.apply)(CouponLineItemType.unapply)
    )
  }

  protected override val formErrorByField = {
    import FormKeys._
    import ApiFormError._

    Map(couponCode -> InvalidLength)
  }
}
