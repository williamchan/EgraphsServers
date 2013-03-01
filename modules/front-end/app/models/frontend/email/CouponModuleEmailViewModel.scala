package models.frontend.email

case class CouponModuleEmailViewModel(
  discountAmount: Int,
  code: String,
  daysUntilExpiration: Int
)