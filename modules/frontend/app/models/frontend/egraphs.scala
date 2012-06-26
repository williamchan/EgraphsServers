//
// Case classes for rendering egraphs
//
package models.frontend.egraphs

import java.util.Date
import xml.Elem
import org.squeryl.Query
import org.joda.money.Money


trait EgraphViewModel {
  def orderId: Long
  def orientation: models.EgraphFrame
  def productUrl: String
  def productTitle: String
  def productDescription: String
  def thumbnailUrl: String
}

case class PendingEgraphViewModel(
  orderStatus: models.enums.orderReviewStatus.EnumVal,
  orderDetails: OrderDetails)
  extends EgraphViewModel

case class FulfilledEgraphViewModel(
  downloadUrl: Option[String],
  publicStatus: models.enums.PrivacyStatus.EnumVal,
  signedTimestamp: String)
  extends EgraphViewModel

case class OrderDetails(
  orderDate: String,
  orderNumber: Long,
  price: Money,
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
  override def render(id: Long, status:String) = {
    val ns = <ul>
      <li>
        <a href="#">View Fullscreen</a>
      </li>
    </ul>

    ns
  }
}

