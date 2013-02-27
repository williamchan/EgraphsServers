package models.checkout.forms.enums

sealed abstract class ApiError(val name: String)


object ApiError extends egraphs.playutils.Enum {
  sealed abstract class EnumVal(name: String) extends ApiError(name) with Value

  //
  // General Form Errors
  //
  val InvalidLength = new EnumVal("invalid_length") {}
  val InvalidFormat = new EnumVal("invalid_format") {}
  val InvalidType = new EnumVal("unexpected_type") {}
  val Required = new EnumVal("required") {}

  //
  // Higher level errors
  //
  val InvalidProduct = new EnumVal("invalid_product") {}
  val NoInventory = new EnumVal("no_inventory") {}
  val InvalidCouponCode = new EnumVal("invalid_code") {}

  //
  // Transaction failures
  //
  val InsufficientInventory = new EnumVal("insufficient_inventory") {}
}
