//
// Case classes for rendering egraphs
//
package models.frontend.egraphs

import java.util.Date;

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
  def render : String
}

object AdminGalleryControl extends GalleryControlRenderer{
  override def render = {
    "AdminGalleryControl"
  }
}

object OwnerGalleryControl extends GalleryControlRenderer{
  override def render  = {
    "OwnerGalleryControl"
  }
}

object OtherGalleryControl extends GalleryControlRenderer{
  override def render = {
    "OtherGalleryControl"
  }
}

