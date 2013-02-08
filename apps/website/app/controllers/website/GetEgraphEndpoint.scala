package controllers.website

import models.AdministratorStore
import models.FulfilledOrder
import models.Order
import models.OrderStore
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Session
import services._
import blobs.AccessPolicy
import mvc.egraphs.EgraphView
import services.http.ControllerMethod
import services.http.EgraphsSession.Conversions._
import services.mvc.ImplicitHeaderAndFooterData
import _root_.frontend.formatting.DateFormatting.Conversions._
import social.Pinterest
import video.EgraphVideoEncoder

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

  //
  // Controllers
  //

  def getEgraph(orderId: Long) = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      orderStore.findFulfilledWithId(orderId) match {
        case None => NotFound("No Egraph exists with the provided identifier.")
        case Some(FulfilledOrder(order, egraph)) =>
          val product = order.product
          val celebrity = product.celebrity
          val mp4Url = egraph.getVideoAsset.getSavedUrl(AccessPolicy.Public)
          val egraphStillUrl = egraph.getEgraphImage(EgraphVideoEncoder.canvasWidth).asJpg.getSavedUrl(AccessPolicy.Public)
          val thisPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)
          val classicPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphClassic(order.id).url)
          val celebrityNameForTweet = celebrity.twitterUsername match {
            case Some(username) => "@" + username
            case None => celebrity.publicName
          }
          val tweetText = "An egraph for " + order.recipientName + " from " + celebrityNameForTweet
          val shareOnPinterestLink = Pinterest.getPinterestShareLink(
            url = thisPageLink,
            media = egraphStillUrl,
            description = celebrity.publicName + " egraph for " + order.recipientName)
          Ok(views.html.frontend.egraph(
            mp4Url = mp4Url,
            videoPosterUrl = egraphStillUrl,
            celebrityName = celebrity.publicName,
            celebrityTagline = celebrity.roleDescription,
            recipientName = order.recipientName,
            privacySetting = order.privacyStatus.name,
            messageToCelebrity = order.messageToCelebrity,
            productIconUrl = product.iconUrl,
            signedOnDate = egraph.getSignedAt.formatDayAsPlainLanguage,
            thisPageLink = thisPageLink,
            classicPageLink = classicPageLink,
            shareOnPinterestLink = shareOnPinterestLink,
            tweetText = tweetText,
            isPromotional = order.isPromotional
          ))
      }
    }
  }

  /**
   * Serves up a single egraph HTML page. The egraph number is actually the number
   * of the associated order, as several attempts to satisfy an egraph could have
   * been made before a successful one was signed.
   */
  def getEgraphClassic(orderId: Long) = controllerMethod.withForm() { implicit authToken =>
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
    Redirect(controllers.routes.WebsiteControllers.getEgraphClassic(orderId))
  }

  private def isViewable(order: Order)(implicit session: Session): Boolean = {
    val customerIdOption = session.customerId.map(customerId => customerId)
    val adminIdOption = session.adminId.map(adminId => adminId)

    order.isPublic ||
      order.isBuyerOrRecipient(customerIdOption) ||
      administratorStore.isAdmin(adminIdOption)
  }
}

