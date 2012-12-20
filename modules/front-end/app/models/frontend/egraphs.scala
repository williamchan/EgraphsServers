//
// Case classes for rendering egraphs
//
package models.frontend.egraphs

import xml.Elem

trait EgraphViewModel {
  def buyerId: Long
  def orderId: Long
  def orientation: String
  def productUrl: String
  def productPublicName: String
  def productTitle: String
  def productDescription: String
  def recipientId: Long
  def recipientName: String
  def thumbnailUrl: String
  def isPending: Boolean
  def isGift: Boolean
}

case class PendingEgraphViewModel(
  buyerId: Long,
  orderStatus: String,
  orderDetails: OrderDetails,
  orderId: Long,
  orientation: String,
  productUrl: String,
  productPublicName: String,
  productTitle: String,
  productDescription: String,
  recipientId: Long,
  recipientName: String,
  thumbnailUrl: String,
  egraphExplanationUrl: String) extends EgraphViewModel {

  val isPending = true

  def isGift: Boolean = {
    buyerId != recipientId
  }
}

case class FulfilledEgraphViewModel(
  buyerId: Long,
  facebookShareLink: String,
  orderId: Long,
  orientation: String,
  productUrl: String,
  productPublicName: String,
  productTitle: String,
  productDescription: String,
  publicStatus: String,
  recipientId: Long,
  recipientName: String,
  signedTimestamp: String,
  thumbnailUrl: String,
  twitterShareLink: String,
  viewEgraphUrl: String) extends EgraphViewModel {

  val isPending = false

  def isGift: Boolean = {
    buyerId != recipientId
  }
}

case class OrderDetails(
  orderDate: String,
  orderNumber: Long,
  price: String,
  statusText: String,
  shippingMethod : String,
  UPSNumber : String)

//Map over option
abstract class GalleryControlRenderer {
  def render(id: Long, status: String) : Elem
}

object AdminGalleryControl extends GalleryControlRenderer{
  override def render(id: Long, status:String) = {
    val ns =
      <ul>
        <li>
          <a href={"/" +id } >View Full Egraph</a>
        </li>
      </ul>

    ns
  }
}
//TODO download link, order prints link
object OwnerGalleryControl extends GalleryControlRenderer{
  override def render(id: Long, status:String)  = {
    val ns =
        <ul>
        <li>
          <a href={"/" +id }>View Full Egraph</a>
        </li>
      </ul>

    ns
  }
}

object OtherGalleryControl extends GalleryControlRenderer{
  override def render(id: Long, status:String) = {
    val ns = 
      <ul>
        <li>
          <a href={"/" +id }>View Full Egraph</a>
        </li>
      </ul>

    ns
  }
}

