package models.checkout.forms

import play.api.data.{Form, Forms}
import models.Coupon
import services.AppConfig
import models.checkout._
import scala.Some


object CouponForm extends CheckoutForm[CouponLineItemType]{

  object FormKeys {
    val _codeLength = Coupon.defaultCodeLength
    val couponCode = "couponCode"
  }

  override val form = Form[CouponLineItemType] {
    import ApiForms._
    import FormKeys._

    Forms.mapping(
      couponCode -> (text verifying validCouponCode)
    )(applyToForm)(unapplyToForm)
  }


  //
  // helpers
  //
  def unapplyToForm(couponItemType: CouponLineItemType) = Some(couponItemType.coupon.code)

  def applyToForm(code: String)(implicit services: CouponLineItemTypeServices = defaultServices) = {
    services.findByCouponCode(code).get
  }

  private def defaultServices = AppConfig.instance[CouponLineItemTypeServices]
}
