package models.checkout.data

import play.api.data.{FormError, Form, Forms}
import models.{CouponServices, CouponStore, Coupon}
import services.AppConfig
import models.checkout._
import org.joda.money.{CurrencyUnit, Money}
import services.db.HasTransientServices
import scala.Some


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
        couponCode -> nonEmptyText(_codeLength, _codeLength) // could use a custom validator to attach form error for invalid code...
      )(applyToForm)(unapplyToForm)
    )
  }

  protected override val formErrorByField = {
    Map(FormKeys.couponCode -> ApiFormError.InvalidLength)
  }


  def applyToForm(code: String)(implicit services: CouponLineItemTypeServices = AppConfig.instance[CouponLineItemTypeServices]) = services.findByCouponCode(code).get
  def unapplyToForm(couponItemType: CouponLineItemType) = Some(couponItemType.coupon.code)
}
