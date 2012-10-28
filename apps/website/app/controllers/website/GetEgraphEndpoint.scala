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
import services.blobs.AccessPolicy
import services.graphics.Handwriting
import services.http.ControllerMethod
import services.http.EgraphsSession.Conversions._
import services.social.Facebook
import services.social.Twitter

private[controllers] trait GetEgraphEndpoint { this: Controller =>
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
  /**
   * Serves up a single egraph HTML page. The egraph number is actually the number
   * of the associated order, as several attempts to satisfy an egraph could have
   * been made before a successful one was signed.
   */
  def getEgraph(orderId: Long) = controllerMethod() {
    Action { implicit request =>
      // Get an order with provided ID
      val session = request.session
      orderStore.findFulfilledWithId(orderId) match {
        case Some(FulfilledOrder(order, egraph)) if isViewable(order)(session) =>          
          val maybeCustomerId = session.customerId
          val maybeGalleryLink = maybeCustomerId.map { customerId =>
            controllers.routes.WebsiteControllers.getCustomerGalleryById(customerId.toLong).url
          }
  
          Ok(GetEgraphEndpoint.html(
            egraph = egraph,
            order = order,
            facebookAppId = facebookAppId,
            galleryLink = maybeGalleryLink,
            consumerApp = consumerApp
          ))
        case Some(FulfilledOrder(order, egraph)) => Forbidden("This Egraph is private.")
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
}

object GetEgraphEndpoint {

  def html(
    egraph: Egraph,
    order: Order,
    penWidth: Double=Handwriting.defaultPenWidth,
    shadowX: Double=Handwriting.defaultShadowOffsetX,
    shadowY: Double=Handwriting.defaultShadowOffsetY,
    facebookAppId: String = "",
    galleryLink: Option[String] = None,
    consumerApp: ConsumerApplication
  )(
    implicit request: RequestHeader
  ): Html = 
  {
    // Get related data model objects
    val product = order.product
    val celebrity = product.celebrity

    // Prepare the framed image
    val frame = product.frame match {
      case PortraitEgraphFrame => PortraitEgraphFrameViewModel
      case LandscapeEgraphFrame => LandscapeEgraphFrameViewModel
    }

    val rawSignedImage = egraph.image(product.photoImage)
    // TODO SER-170 this code is quite similar to that in GalleryOrderFactory.
    // Refactor together and put withSigningOriginOffset inside EgraphImage.
    val frameFittedImage = rawSignedImage
      .withPenWidth(penWidth)
      .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
      .withPenShadowOffset(shadowX, shadowY)
      .scaledToWidth(frame.imageWidthPixels)

    val svgzImageUrl = frameFittedImage.getSavedUrl(AccessPolicy.Public)
    val rasterImageUrl = frameFittedImage.rasterized.getSavedUrl(AccessPolicy.Public)

    // Prepare the icon
    val icon = product.icon
    val frameFittedIconUrl = icon.resized(Product.minIconWidth, Product.minIconWidth).getSaved(AccessPolicy.Public).url

    // Prepare the story
    val story = egraph.story(celebrity, product, order)

    // Signed at date
    val formattedSigningDate = new SimpleDateFormat("MMMM dd, yyyy").format(egraph.getSignedAt)

    // Social links
    val thisPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)

    val facebookShareLink = Facebook.getEgraphShareLink(fbAppId = facebookAppId,
      fulfilledOrder = FulfilledOrder(order = order, egraph = egraph),
      thumbnailUrl = rasterImageUrl,
      viewEgraphUrl = thisPageLink)

    val twitterShareLink = Twitter.getEgraphShareLink(celebrity = celebrity, viewEgraphUrl = thisPageLink)

    // Render
    views.html.frontend.egraph(
      signerName = celebrity.publicName,
      recipientName = order.recipientName,
      frameCssClass = frame.cssClass,
      frameLayoutColumns = frame.cssFrameColumnClasses,
      productIcon = frameFittedIconUrl,
      storyLayoutColumns = frame.cssStoryColumnClasses,
      storyTitle = product.storyTitle,
      storyBody = story.body,
      audioUrl = egraph.assets.audioMp3Url,
      signedImage = svgzImageUrl,
      signedOnDate = formattedSigningDate,
      shareOnFacebookLink = facebookShareLink,
      shareOnTwitterLink = twitterShareLink,
      galleryLink = galleryLink
    )
  }

  def url(orderId: Long): String = {
    controllers.routes.WebsiteControllers.getEgraph(orderId).url
  }
}