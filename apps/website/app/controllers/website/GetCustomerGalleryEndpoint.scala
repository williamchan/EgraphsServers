package controllers.website

import services.http.{AccountRequestFilters, SafePlayParams, ControllerMethod}
import play.mvc.Controller
import models._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, OrderReviewStatus, EgraphState, PublishedStatus}
import models.frontend.egraphs._
import play.mvc.Scope.Session
import scala.Some
import models.Egraph
import services.blobs.AccessPolicy
import scala.Some
import scala.Some
import java.text.SimpleDateFormat


private[controllers] trait GetCustomerGalleryEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def customerStore: CustomerStore
  protected def administratorStore: AdministratorStore
  protected def orderStore: OrderStore
  protected def accountRequestFilters: AccountRequestFilters

  import SafePlayParams.Conversions._

  def getCustomerGallery(galleryCustomerId: Long) = controllerMethod() {

    accountRequestFilters.requireValidCustomerId(galleryCustomerId){ customer =>
      //TODO sbilstein moves these functions into separate areas for testing
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

      //The type system makes me angry here, Factory takes iterables of the filtered queries
      //need to add some filtering according to privacy status.
      val orders:List[EgraphViewModel] = galleryControl match {
        case AdminGalleryControl | OwnerGalleryControl =>
          GalleryOrderFactory.makePendingEgraphViewModel(pendingOrders).toList ++
            GalleryOrderFactory.makeFulfilledEgraphViewModel(fulfilledOrders).flatten.toList
        case _ =>
          GalleryOrderFactory.makeFulfilledEgraphViewModel(fulfilledOrders).flatten.toList

      }
      views.frontend.html.account_gallery(customer.username, orders, galleryControl)
    }
  }
}


