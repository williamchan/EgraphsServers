package controllers.website

import java.text.SimpleDateFormat
import models.AdministratorStore
import models.Egraph
import models.FulfilledOrder
import models.LandscapeEgraphFrame
import models.Order
import models.OrderStore
import models.PortraitEgraphFrame
import models.Product
import models.frontend.egraph.LandscapeEgraphFrameViewModel
import models.frontend.egraph.PortraitEgraphFrameViewModel
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import play.api.mvc.Session
import play.api.templates.Html
import services._
import mvc.egraphs.EgraphView
import services.blobs.AccessPolicy
import services.graphics.Handwriting
import services.http.ControllerMethod
import services.http.EgraphsSession.Conversions._
import services.social.Facebook
import services.social.Twitter
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetEgraphEndpoint extends ImplicitHeaderAndFooterData { 
  this: Controller =>
  
  //
  // Services
  //
  protected def administratorStore: AdministratorStore
  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod
  protected def facebookAppId: String
  protected def consumerApp: ConsumerApplication

  val penWidth: Double=Handwriting.defaultPenWidth
  val shadowX: Double=Handwriting.defaultShadowOffsetX
  val shadowY: Double=Handwriting.defaultShadowOffsetY

  //
  // Controllers
  //
  /**
   * Serves up a single egraph HTML page. The egraph number is actually the number
   * of the associated order, as several attempts to satisfy an egraph could have
   * been made before a successful one was signed.
   */
  def getEgraph(orderId: Long) = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      // Get an order with provided ID
      val session = request.session
      orderStore.findFulfilledWithId(orderId) match {
        case Some(FulfilledOrder(order, egraph)) if isViewable(order)(session) => {
          val maybeCustomerId = session.customerId
          val maybeGalleryLink = maybeCustomerId.map { customerId =>
            controllers.routes.WebsiteControllers.getCustomerGalleryById(customerId).url
          }
          Ok(EgraphView.renderEgraphPage(egraph=egraph, order=order, facebookAppId = facebookAppId,galleryLink = maybeGalleryLink, consumerApp = consumerApp))
        }
        case Some(FulfilledOrder(order, egraph)) => Forbidden(views.html.frontend.errors.forbidden())
        case None => NotFound("No Egraph exists with the provided identifier.")
      }
    }
  }

  /** Redirects the old egraph url /egraph/{orderId} to the current url */
  def getEgraphRedirect(orderId: Long) = Action {
    Redirect(controllers.routes.WebsiteControllers.getEgraph(orderId))
  }

  private def isViewable(order: Order)(implicit session: Session): Boolean = {
    val customerIdOption = session.customerId.map(customerId => customerId.toLong)
    val adminIdOption = session.adminId.map(adminId => adminId.toLong)

    order.isPublic ||
      order.isBuyerOrRecipient(customerIdOption) ||
      administratorStore.isAdmin(adminIdOption)
  }

  private def url(orderId: Long): String = {
    controllers.routes.WebsiteControllers.getEgraph(orderId).url
  }

}

