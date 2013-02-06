package models.frontend.email

trait EgraphSignedEmailViewModel {
  def viewEgraphUrl: String
  def celebrityPublicName: String
  def recipientName: String
  def couponAmount: Int
  def couponCode: String
  def buyerName: String
  def isGift: Boolean
}

case class RegularEgraphSignedEmailViewModel(
  viewEgraphUrl: String,
  celebrityPublicName: String,
  recipientName: String,
  couponAmount: Int,
  couponCode: String) extends EgraphSignedEmailViewModel {
  
  val isGift = false
  val buyerName = recipientName
}
  
case class GiftEgraphSignedEmailViewModel(
  viewEgraphUrl: String,
  celebrityPublicName: String,
  recipientName: String,
  couponAmount: Int,
  couponCode: String,  
  buyerName: String) extends EgraphSignedEmailViewModel {
  
  val isGift = true
}