package models.checkout.forms

import play.api.data.{Form, Forms}
import models.Coupon
import services.AppConfig
import models.checkout._
import scala.Some


object CouponForm extends CheckoutForm[Option[CouponLineItemType]]{

  object FormKeys {
    val _codeLength = Coupon.defaultCodeLength
    val couponCode = "couponCode"
  }

  override val form = Form[Option[CouponLineItemType]] {
    import ApiForms._
    import FormKeys._
    import play.api.data.Forms.optional

    Forms.mapping(
      couponCode -> optional(text verifying validCouponCode)
    )(applyToForm)(unapplyToForm)
  }


  //
  // helpers
  //
  def unapplyToForm(couponItemType: Option[CouponLineItemType]) = couponItemType.map(lit => Some(lit.coupon.code))

  def applyToForm(maybeCode: Option[String])(implicit services: CouponLineItemTypeServices = defaultServices) = {
    for (
      code <- maybeCode;
      coupon <- services.findByCouponCode(code)
    ) yield {
      coupon
    }
  }

  private def defaultServices = AppConfig.instance[CouponLineItemTypeServices]
}
