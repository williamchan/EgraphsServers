//
// Case classes for rendering egraphs
//
package models.frontend.egraphs

import xml.Elem


trait EgraphViewModel {
  def orderId: Long
  def orientation: String
  def productUrl: String
  def productPublicName: Option[String]
  def productTitle: String
  def productDescription: String
  def thumbnailUrl: String
}

case class PendingEgraphViewModel(
  orderStatus: String,
  orderDetails: OrderDetails,
  orderId: Long,
  orientation: String,
  productUrl: String,
  productPublicName: Option[String],
  productTitle: String,
  productDescription: String,
  thumbnailUrl: String) extends EgraphViewModel

case class FulfilledEgraphViewModel(
  downloadUrl: Option[String],
  publicStatus: String,
  signedTimestamp: String,
  fbAppId: String,
  redirectURI: String,
  orderId: Long,
  orientation: String,
  productUrl: String,
  productPublicName: Option[String],
  productTitle: String,
  productDescription: String,
  thumbnailUrl: String) extends EgraphViewModel

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
        <a href="#">View Fullscreen</a>
      </li>
      <li>
        <a href="#">Download</a>
      </li>
      <li>
        <a href="#">Order Prints</a>
      </li>
    </ul>

    ns
  }
}

object OwnerGalleryControl extends GalleryControlRenderer{
  override def render(id: Long, status:String)  = {
    val ns =
        <ul>
        <li>
          <a href="#">View Fullscreen</a>
        </li>
        <li>
          <a href="#">Download</a>
        </li>
        <li>
          <a href="#">Order Prints</a>
        </li>
      </ul>

    ns
  }
}

object OtherGalleryControl extends GalleryControlRenderer{
  override def render(id: Long, status:String) = {
    val ns = <ul>
      <li>
        <a href="#">View Fullscreen</a>
      </li>
    </ul>

    ns
  }
}

