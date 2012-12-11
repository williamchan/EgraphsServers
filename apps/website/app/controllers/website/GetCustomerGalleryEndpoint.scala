package controllers.website

import play.api._
import play.api.mvc._
import models._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, EgraphState}
import models.frontend.egraphs._
import services.mvc.ImplicitHeaderAndFooterData
import models.GalleryOrderFactory
import services.ConsumerApplication
import services.http.{SafePlayParams, ControllerMethod}
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import egraphs.authtoken.AuthenticityToken

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
  protected def httpFilters: HttpFilters
  protected def facebookAppId: String
  protected def consumerApp: ConsumerApplication

  import SafePlayParams.Conversions._

  def getCustomerGalleryByUsername(username: String) = controllerMethod.withForm() 
  { implicit authToken =>
    httpFilters.requireCustomerUsername(username) { customer =>
      Action { implicit request =>
        serveCustomerGallery(customer)
      }
    }
  }

  def getCustomerGalleryById(galleryCustomerId: Long) = controllerMethod.withForm() 
  { implicit authToken =>
    httpFilters.requireCustomerLogin(galleryCustomerId) { case (customer, account )=>
      Action { implicit request =>
        serveCustomerGallery(customer)
      }
    }
  }

  def serveCustomerGallery[A]
    (customer: Customer)
    (implicit request: RequestHeader, authToken: AuthenticityToken)
  : Result = 
  {    
    val galleryCustomerId = customer.id
    //If admin is true admin
    val session = request.session
    val adminGalleryControlOption = for(
      sessionAdminId <- session.adminId.map(adminId => adminId.toLong);
      adminOption   <- administratorStore.findById(sessionAdminId)) yield AdminGalleryControl
    //if customerId is the same as the gallery requested
    val sessionGalleryControlOption = for(
      sessionCustomerId <- session.customerId.map(customerId => customerId.toLong);
      if (sessionCustomerId == galleryCustomerId)) yield OwnerGalleryControl

    //In priority order
    val galleryControlPrecedence = List(
      adminGalleryControlOption,
      sessionGalleryControlOption,
      Some(OtherGalleryControl)
    )
    //Pop off control with highest precedence
    val galleryControl = galleryControlPrecedence.flatten.head

    //get orders (my pending orders and my gift orders to both be displayed)
    val ordersAndEgraphs = orderStore.galleryOrdersWithEgraphs(galleryCustomerId).toList
    val giftOrdersAndEgraphs = orderStore.galleryGiftOrdersWithEgraphs(galleryCustomerId).toList

    val pendingOrders = GalleryOrderFactory.filterPendingOrders(ordersAndEgraphs)
    val pendingGiftOrders = GalleryOrderFactory.filterPendingOrders(giftOrdersAndEgraphs)
    
    val fulfilledOrders = getFulfilledOrders(ordersAndEgraphs)
    val fulfilledGiftOrders = getFulfilledOrders(giftOrdersAndEgraphs)
    
    val orders = getOrders(pendingOrders, fulfilledOrders, galleryControl, customer)
    val giftOrders = getOrders(pendingGiftOrders, fulfilledGiftOrders, galleryControl, customer)
    
    Ok(views.html.frontend.account_gallery(customer.username, orders, giftOrders, galleryControl))
  }
  
  /**
   * Creates and returns a list of EgraphViewModels, which can then be used to properly
   * display the Egraph and related information on the user's gallery page.
   */
  private def getOrders
    (pendingOrders: List[(Order, Option[Egraph])], fulfilledOrders: List[(Order, Option[Egraph])], 
       galleryControl: GalleryControlRenderer, customer: Customer)
    (implicit request: RequestHeader, authToken: AuthenticityToken)
  : List[EgraphViewModel] = 
  {
    galleryControl match {
      case AdminGalleryControl | OwnerGalleryControl => {
        GalleryOrderFactory.makePendingEgraphViewModel(pendingOrders).toList ++
          GalleryOrderFactory.makeFulfilledEgraphViewModel(fulfilledOrders, facebookAppId, consumerApp).flatten.toList          
      }
      case _ =>
        if (customer.isGalleryVisible){
          GalleryOrderFactory.makeFulfilledEgraphViewModel(fulfilledOrders.filter(
            orderAndOption => {orderAndOption._1.privacyStatus == PrivacyStatus.Public}), 
            facebookAppId,
            consumerApp).flatten.toList
        } else {
          List()
        }
    }
  }
  
  /**
   * Filters the list of possible orders and Egraphs, returning only those which are
   * already fulfilled.
   */
  private def getFulfilledOrders(ordersAndEgraphs: List[(Order, Option[Egraph])]): List[(Order, Option[Egraph])] = {
    for (
      orderAndMaybeEgraph <- ordersAndEgraphs;
      publishedEgraph <- orderAndMaybeEgraph._2 if publishedEgraph.egraphState == EgraphState.Published
    ) yield {
      orderAndMaybeEgraph
    }      
  }
}