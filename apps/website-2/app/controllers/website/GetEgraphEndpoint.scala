package controllers.website

import play.api._
import play.api.mvc._
import services.blobs.AccessPolicy
import services.http.ControllerMethod
import play.api.templates.Html
import services.http.SafePlayParams.Conversions.paramsToOptionalParams
import services.graphics.Handwriting
import models._
import controllers.WebsiteControllers
import frontend.egraph.{LandscapeEgraphFrameViewModel, PortraitEgraphFrameViewModel}
import play.api.mvc.Results.Redirect
import services.social.{Twitter, Facebook}
import java.text.SimpleDateFormat
import services.Utils
import services.http.EgraphsSession

private[controllers] trait GetEgraphEndpoint { this: Controller =>
  //
  // Services
  //
  protected def administratorStore: AdministratorStore
  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod
  protected def facebookAppId: String

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
      //TODO: PLAY20: from myyk, I did this wrong, these should be from a Form, I didn't know when I did this.  I would do now, but it is 6:30 on a Friday.
      // TODO: disable pen-width and shadow re-setting once we find a good value.
      val params = request.queryString
      
      val penWidth = Utils.getFromMapFirstInSeqOrElse("penWidth", "5.0", params).toDouble
      val shadowX = Utils.getFromMapFirstInSeqOrElse("shadowX", "3.0", params).toDouble
      val shadowY = Utils.getFromMapFirstInSeqOrElse("shadowY", "3.0", params).toDouble
  
      // Get an order with provided ID
      val session = request.session
      orderStore.findFulfilledWithId(orderId) match {
        case Some(FulfilledOrder(order, egraph)) if isViewable(order)(session) =>          
          val maybeCustomerId = session.get(EgraphsSession.Key.CustomerId.name)
          val maybeGalleryLink = maybeCustomerId.map { customerId =>
            controllers.routes.WebsiteControllers.getCustomerGalleryById(customerId.toLong).url
          }
  
          Ok(GetEgraphEndpoint.html(
            egraph = egraph,
            order = order,
            penWidth = penWidth,
            shadowX = shadowX,
            shadowY = shadowY,
            facebookAppId = facebookAppId,
            galleryLink = maybeGalleryLink
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
    val customerIdOption = session.get(EgraphsSession.Key.CustomerId.name).map(customerId => customerId.toLong)
    val adminIdOption = session.get(EgraphsSession.Key.AdminId.name).map(adminId => adminId.toLong)

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
    galleryLink: Option[String] = None
  ): Html = {
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
    val thisPageAction = ""//controllers.routes.WebsiteControllers.getEgraph(order.id.toString).url
    val thisPageLink = Utils.absoluteUrl(thisPageAction)

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