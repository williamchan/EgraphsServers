package controllers.website

import services.http.{AccountRequestFilters, SafePlayParams, ControllerMethod}
import play.mvc.Controller
import play.templates.Html
import models._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, OrderReviewStatus, EgraphState, PublishedStatus}
import models.frontend.egraphs._
import play.mvc.Scope.Session
import scala.Some
import models.Egraph
import services.blobs.AccessPolicy
import scala.Some
import models.Egraph
import scala.Some
import models.Egraph


private[controllers] trait GetCustomerGalleryEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def customerStore: CustomerStore
  protected def administratorStore: AdministratorStore
  protected def orderStore: OrderStore
  protected def accountRequestFilters: AccountRequestFilters

  import SafePlayParams.Conversions._

  def getCustomerGallery(galleryCustomerId: Long) = controllerMethod() {

    accountRequestFilters.requireValidCustomerId(galleryCustomerId){ customer =>

      val adminGalleryControlOption = for(
        sessionAdminId <- session.getLongOption(WebsiteControllers.adminIdKey);
        adminOption   <- administratorStore.findById(sessionAdminId)) yield AdminGalleryControl

      val sessionGalleryControlOption = for(
        sessionCustomerId <- session.getLongOption(WebsiteControllers.customerIdKey);
        if (sessionCustomerId == galleryCustomerId)) yield OwnerGalleryControl

      //Determine appropriate gallery control
      val galleryControlPrecedence = List(
        adminGalleryControlOption,
        sessionGalleryControlOption,
        Some(OtherGalleryControl)
      )

      //Pop off control with highest precedence
      val galleryControl = galleryControlPrecedence.flatten.head

      //Left outerjoin on orders and egraphs
      val orders_and_egraphs = orderStore.getEgraphsAndOrders(galleryCustomerId)

      val pendingOrders = orders_and_egraphs.filter(orderEgraph =>
        orderEgraph._1.reviewStatus != OrderReviewStatus.ApprovedByAdmin)
      val fulfilledOrders = orders_and_egraphs.filter(orderEgraph =>
        orderEgraph._1.reviewStatus == OrderReviewStatus.ApprovedByAdmin)

      val orders = galleryControl match {
        case AdminGalleryControl | OwnerGalleryControl =>
            GalleryEgraphFactory.makePendingEgraphViewModel(pendingOrders.toList) ++
            GalleryEgraphFactory.makeFulfilledEgraphViewModel(fulfilledOrders.toList)

        case OtherGalleryControl =>
          GalleryEgraphFactory.makeFulfilledEgraphViewModel(fulfilledOrders.toList)
    }

      views.frontend.html.account_gallery(customer.username, orders, galleryControl)
    }

  }
}

object GalleryEgraphFactory {

  def makeFulfilledEgraphViewModel(orders:List[(Order, Option[Egraph])]) :
    List[FulfilledEgraphViewModel] = {
      for((order, optionEgraph) <- orders;
                         egraph <- optionEgraph)
      yield {
        val product = order.product
        val rawImage = egraph.image(product.photoImage).scaledToWidth(product.frame.thumbnailWidthPixels)
        FulfilledEgraphViewModel(
          orderId = order.id,
          orientation = product.frame.name,
          productUrl = "//" + product.celebrity.urlSlug + "/" + product.urlSlug,
          productTitle = product.storyTitle,
          productDescription = product.description,
          thumbnailUrl = rawImage.getSavedUrl(accessPolicy = AccessPolicy.Private),
          downloadUrl = Option("egraph/" + order.id),
          publicStatus = order.privacyStatus.name,
          signedTimestamp = egraph.created.toString
        )
      }
  }

  def makePendingEgraphViewModel(orders: List[(Order, Option[Egraph])]) : List[PendingEgraphViewModel] = {
    for((order, optionEgraph) <- orders) yield {
      val product = order.product
      PendingEgraphViewModel(
        orderId = order.id,
        orientation = product.frame.name,
        productUrl = "//" + product.celebrity.urlSlug + "/" + product.urlSlug,
        productTitle = product.storyTitle,
        productDescription = product.description,
        thumbnailUrl = "",
        orderStatus = order.reviewStatus.name,
        orderDetails = new OrderDetails(
          orderDate = order.created.toString(),
          orderNumber = order.id,
          price = order.amountPaid.toString(),
          statusText = "",
          shippingMethod = "",
          UPSNumber = ""
        )
      )
    }
  }
}
