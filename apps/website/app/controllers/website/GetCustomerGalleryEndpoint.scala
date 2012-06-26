package controllers.website

import services.http.{AccountRequestFilters, AdminRequestFilters, SafePlayParams, ControllerMethod}
import play.mvc.Controller
import play.templates.Html
import frontend.egraphs._
import models._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, OrderReviewStatus, EgraphState, PublishedStatus}
import frontend.egraphs._
import frontend.egraphs.PendingEgraphViewModel
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
  protected def accountFilters: AccountRequestFilters

  import SafePlayParams.Conversions._

  def getCustomerGallery(galleryCustomerId: String) = controllerMethod() {

    requireValidCustomerId(galleryCustomerId){
      //iterator, if
      val adminGalleryControlOption = for(
        sessionAdminId <- session.getLongOption(WebsiteControllers.adminIdKey);
        adminOption   <- administratorStore.findById(sessionAdminId)) yield AdminGalleryControl

      val sessionGalleryControlOption = for(
        sessionCustomerId <- session.getLongOption(WebsiteControllers.customerIdKey);
        if (sessionCustomerId == galleryCustomerId)) yield OwnerGalleryControl

      //Determine appropiate gallery control
      val galleryControlPrecedence = List(
        adminGalleryControlOption,
        sessionGalleryControlOption,
        Some(OtherGalleryControl)
      )
      //Remove the Nones and get the highest precendece control
      val galleryControl = galleryControlPrecedence.flatten.head

      val orders_and_egraphs = orderStore.getEgraphsAndOrders(galleryCustomerId.toLong)

      val pendingOrders = orders_and_egraphs.filter((order:Order, egraphOption:Option[Egraph]) => order.reviewStatus != OrderReviewStatus.ApprovedByAdmin)
      val fulfilledOrders = orders_and_egraphs.filter((order:Order, egraphOption:Option[Egraph]) => order.reviewStatus == OrderReviewStatus.ApprovedByAdmin)

      val orders = galleryControl match {
        case AdminGalleryControl | OwnerGalleryControl =>
          List(pendingOrders.map((order:Order, egraphOption:Option[Egraph]) => GalleryEgraphFactory.makeView(order, egraphOption)),
               fulfilledOrders.map((order:Order, egraphOption:Option[Egraph]) => GalleryEgraphFactory.makeView(order, egraphOption)))
        case OtherGalleryControl =>
          List(fulfilledOrders.filter((order:Order, egraphOption:Option[Egraph]) => egraphOption
          )
      }

      views.frontend.html.account_gallery(customer.username, List(), galleryControl)
    }

  }
}

object GalleryEgraphFactory {

  def makeFulfilledEgraphViewModel(orders:Traversable[(Order, Option[Egraph])], privacyFilter: PrivacyStatus.type) :
    List[FulfilledEgraphViewModel] = {
    List(for((order, optionEgraph) <- orders; egraph <- optionEgraph;)
    {
      val product = order.product
      val rawImage = egraph.image(product.photoImage).scaledToWidth(product.frame.thumbnailWidthPixels)
      FulfilledEgraphViewModel(
        orderId = order.id,
        orientation = product.frame,
        productUrl = "//" + product.celebrity.urlSlug + "/" + product.urlSlug,
        productTitle = product.storyTitle,
        productDescription = product.description
        thumbnailUrl = rawImage.getSavedUrl(accessPolicy = AccessPolicy.Private),
        downloadUrl = Option("egraph/" + order.id),
        publicStatus = order.privacyStatus,
        signedTimeStamp = egraph.created.toString
       )
    })
  }

  def makePendingOrderEgraphViewModel(orders: Traversable[(Order, Option[Egraph])]) : List[PendingEgraphViewModel] = {
    List(for((order, optionEgraph) <- orders;){
      val product = order.product
      PendingEgraphViewModel(
        orderId = order.id,
        orientation = product.frame,
        productUrl = "//" + product.celebrity.urlSlug + "/" + product.urlSlug,
        productTitle = product.storyTitle,
        productDescription = product.description,
        thumbnailUrl = "",
        orderStatus = order.get_reviewStatus(),
        orderDetails = OrderDetails(
          orderDate = order.created.toString(),
          orderNumber = order.id,
          price = order.amountPaid,
          statusText = "",
          shippingMethod = "",
          UPSNumber = ""
        )
      )
    })
  }

  def makeView(order: Order, optionEgraph : Option[Egraph]) : models.frontend.egraphs.Egraph = {
    val product = order.product
    val viewEgraph = models.frontend.egraphs.Egraph(
      productUrl = "/" + product.celebrity.urlSlug + "/" + product.urlSlug ,
      downloadUrl = Option("egraph/" + order.id),
      orderUrl = "orders/" + order.id + "/confirm",
      orientation = product.frame,
      productDescription = product.description,
      productTitle = product.storyTitle,
      id =  order.id,
      publicStatus = order.privacyStatus,
      orderStatus = order.reviewStatus)

    optionEgraph match {
      case Some(egraph) => egraph match {
        case egraph if(egraph.isPublished) => {
          //works for both orientations
          val rawImage = egraph.image(product.photoImage).scaledToWidth(product.frame.thumbnailWidthPixels)
          viewEgraph.thumbnailUrl = rawImage.getSavedUrl(accessPolicy = AccessPolicy.Private)
          viewEgraph.signedTimestamp = egraph.created.toString()
        }
        case _ => {
          //Set up as pending order
          viewEgraph.orderDetails = models.frontend.egraphs.OrderDetails(
            orderDate = order.created.toString(),
            orderNumber = order.id,
            price = order.amountPaid,
            statusText = "Pending",
            shippingMethod = "",
            UPSNumber = ""
          )
        }
      }
      case None =>  {
        //Set up as pending order
        viewEgraph.orderDetails = models.frontend.egraphs.OrderDetails(
          orderDate = order.created.toString(),
          orderNumber = order.id,
          price = order.amountPaid,
          statusText = "Pending",
          shippingMethod = "",
          UPSNumber = ""
        )
      }
    }
    viewEgraph
  }
}

object GetCustomerGalleryEndpoint {
  def html(username: String, modelEgraphs: List[Egraph]): Html = {
    //convert egraphs to view egraphs
    //convert role to owner, other, or admin control objects
    views.frontend.html.account_gallery(username, List(),  OwnerGalleryControl)
  }
}
