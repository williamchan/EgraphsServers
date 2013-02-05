package models.frontend.email

trait EgraphSignedEmailViewModel {
  def viewEgraphUrl: String
  def celebrityPublicName: String
  def recipientName: String
  def buyerName: String
  def isGift: Boolean
}

case class RegularEgraphSignedEmailViewModel(
  viewEgraphUrl: String,
  celebrityPublicName: String,
  recipientName: String) extends EgraphSignedEmailViewModel {
  
  val isGift = false
  val buyerName = recipientName
}
  
case class GiftEgraphSignedEmailViewModel(
  viewEgraphUrl: String,
  celebrityPublicName: String,
  recipientName: String,
  buyerName: String) extends EgraphSignedEmailViewModel {
  
  val isGift = true
}