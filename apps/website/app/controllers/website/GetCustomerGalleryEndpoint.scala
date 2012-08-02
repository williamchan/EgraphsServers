package controllers.website

import services.http.{SafePlayParams, AccountRequestFilters, ControllerMethod}
import services.Utils
import play.mvc.Controller
import models._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, EgraphState}
import models.frontend.egraphs._
import scala.Some
import services.mvc.ImplicitHeaderAndFooterData

/**
 * Controller for displaying customer galleries. Galleries serve as a "wall of egraphs".
 * The main functionality of this controller is to determine what level of control and visibility
 * the requesting user has over the egrpahs being shown.
 *
 * Admin-> An admin should be able to see any egraph or pending order and any details associated with it.
 * Owner-> An owner should be able to see any egraphs they own and any pending orders.
 * Others->Some user, logged in or not, should be able to see only fulfilled egraphs set as public by the Owner.
 *
 **/


private[controllers] trait GetCustomerGalleryEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def customerStore: CustomerStore
  protected def administratorStore: AdministratorStore
  protected def orderStore: OrderStore
  protected def accountRequestFilters: AccountRequestFilters
  protected def facebookAppId: String

  import SafePlayParams.Conversions._

  def getCustomerGalleryByUsername(username: String) = controllerMethod() {
    accountRequestFilters.requireValidCustomerUsername(username) {
      customer => serveCustomerGallery(customer)
    }
  }

  def getCustomerGalleryById(galleryCustomerId: Long) = controllerMethod() {
    accountRequestFilters.requireValidCustomerId(galleryCustomerId){
      customer => serveCustomerGallery(customer)
    }
  }

  def serveCustomerGallery(customer:Customer) : Any = {
    val galleryCustomerId = customer.id
    //If admin is true admin
    val adminGalleryControlOption = for(
      sessionAdminId <- session.getLongOption(WebsiteControllers.adminIdKey);
      adminOption   <- administratorStore.findById(sessionAdminId)) yield AdminGalleryControl
    //if customerId is the same as the gallery requested
    val sessionGalleryControlOption = for(
      sessionCustomerId <- session.getLongOption(WebsiteControllers.customerIdKey);
      if (sessionCustomerId == galleryCustomerId)) yield OwnerGalleryControl

    //In priority order
    val galleryControlPrecedence = List(
      adminGalleryControlOption,
      sessionGalleryControlOption,
      Some(OtherGalleryControl)
    )
    //Pop off control with highest precedence
    val galleryControl = galleryControlPrecedence.flatten.head


    //get orders
    val orders_and_egraphs = orderStore.getEgraphsAndOrders(galleryCustomerId).toList

    val pendingOrders = orders_and_egraphs.filter(orderEgraph => {
      val maybeEgraph = orderEgraph._2
      maybeEgraph match {
        case None => true
        case Some(egraph) => egraph.egraphState match {
          case EgraphState.Published => false
          case EgraphState.RejectedByAdmin => false
          case EgraphState.FailedBiometrics => false
          case _ => true
        }
      }
    })

    val fulfilledOrders = orders_and_egraphs.filter(orderEgraph =>
      orderEgraph._2.map(_.egraphState) == Some(EgraphState.Published)
    )


    val orders:List[EgraphViewModel] = galleryControl match {
      case AdminGalleryControl | OwnerGalleryControl =>
        GalleryOrderFactory.makePendingEgraphViewModel(pendingOrders).toList ++
          GalleryOrderFactory.makeFulfilledEgraphViewModel(fulfilledOrders, facebookAppId).flatten.toList
      case _ =>
        if (customer.isGalleryVisible){
          GalleryOrderFactory.makeFulfilledEgraphViewModel(fulfilledOrders.filter(
            orderAndOption => {
              orderAndOption._1.privacyStatus == PrivacyStatus.Public
            }), facebookAppId).flatten.toList
        } else {
          List()
        }
    }
    views.frontend.html.account_gallery(customer.username, orders, galleryControl)
  }

}


