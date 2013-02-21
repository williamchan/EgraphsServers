package models.frontend.email

trait ViewEgraphEmailViewModel {
  def viewEgraphUrl: String
  def celebrityPublicName: String
  def recipientName: String
  def buyerName: String
  def isGift: Boolean
}

case class RegularViewEgraphEmailViewModel(
  viewEgraphUrl: String,
  celebrityPublicName: String,
  recipientName: String) extends ViewEgraphEmailViewModel {
  
  val isGift = false
  val buyerName = recipientName
}
  
case class GiftViewEgraphEmailViewModel(
  viewEgraphUrl: String,
  celebrityPublicName: String,
  recipientName: String,
  buyerName: String) extends ViewEgraphEmailViewModel {
  
  val isGift = true
}