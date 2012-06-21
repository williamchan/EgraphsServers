//
// Case classes for rendering egraphs
//
package models.frontend.egraphs

import java.util.Date
import xml.Elem
;

case class Egraph(
  productUrl:String,
  downloadUrl: String,
  orderUrl: String,
  thumbnailUrl: String  = "http://placehold.it/500x400",
  orientation: String = "landscape",
  productDescription:String,
  productTitle: String,
  signedTimestamp: Date,
  publishedStatus: String = "unpublished",
  orderStatus: String = "pending",
  orderDetails: Option[OrderDetails] = None)

case class OrderDetails(
  orderDate: Date,
  orderNumber: Int,
  price: Float,
  statusText: String,
  shippingMethod : String,
  UPSNumber : String)

abstract class GalleryControlRenderer {
  def render : Elem
}

object AdminGalleryControl extends GalleryControlRenderer{
  override def render = {
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
  override def render  = {
    val ns = <ul>
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
  override def render = {
    val ns = <ul>
      <li>
        <a href="#">View Fullscreen</a>
      </li>
    </ul>

    ns
  }
}

