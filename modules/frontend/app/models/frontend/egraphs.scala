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
  id:Int = 0,
  signedTimestamp: String,
  publishedStatus: String = "unpublished",
  orderStatus: String = "pending",
  orderDetails: Option[OrderDetails] = None)

case class OrderDetails(
  orderDate: String,
  orderNumber: Int,
  price: String,
  statusText: String,
  shippingMethod : String,
  UPSNumber : String)

abstract class GalleryControlRenderer {
  def render(id: Int, status: String) : Elem
}

object AdminGalleryControl extends GalleryControlRenderer{
  override def render(id: Int, status:String) = {
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
  override def render(id: Int, status:String)  = {
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
      <li>
        status
      </li>
    </ul>
    ns
  }
}

object OtherGalleryControl extends GalleryControlRenderer{
  override def render(id: Int, status:String) = {
    val ns = <ul>
      <li>
        <a href="#">View Fullscreen</a>
      </li>
    </ul>

    ns
  }
}

