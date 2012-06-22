package controllers.website

import services.http.{SafePlayParams, ControllerMethod}
import play.mvc.Controller
import play.templates.Html
import models.frontend.egraphs.{AdminGalleryControl, OtherGalleryControl, OwnerGalleryControl}
import models._
import controllers.WebsiteControllers
import play.mvc.Scope.Session
import scala.Some
import models.Egraph

private[controllers] trait GetCustomerGalleryEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def customerStore: CustomerStore
  protected def administratorStore: AdministratorStore
  protected def egraphStore: EgraphStore

  import SafePlayParams.Conversions._

  def getCustomerGallery(customerId: String) = controllerMethod() {

    val customer  = customerStore.findById(customerId.toLong) match {
      case Some(customer) => customer
      case None => NotFound("User not found")
    }

    val sessionCustomerIdOption  = session.getLongOption(WebsiteControllers.customerIdKey)

    val adminIdOption = session.getLongOption(WebsiteControllers.adminIdKey)


    val galleryControl = adminIdOption match {
      //Admin logged in, if admin is true admin respond with correct control, otherwise fail to other
      case _ if(administratorStore.isAdmin(adminIdOption)) =>  AdminGalleryControl
      case None => sessionCustomerIdOption match {
        case Some(sessionCustomerId) => sessionCustomerId match {
          case _ if(customerId == sessionCustomerId) => OwnerGalleryControl
          case _ => OtherGalleryControl
        }
        case None => OtherGalleryControl
      }
      case _ => OtherGalleryControl
    }

    val egraphs = galleryControl match {
      case AdminGalleryControl => { }
      case OwnerGalleryControl => { }
      case OtherGalleryControl => { customer }
    }

  }


}

object GetCustomerGalleryEndpoint {
  def html(username: String, modelEgraphs: List[Egraph]): Html = {
    //convert egraphs to view egraphs
    //convert role to owner, other, or admin control objects
    views.frontend.html.account_gallery(username, List(),  OwnerGalleryControl)
  }
}
