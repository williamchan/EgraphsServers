package controllers.website

import play.mvc.Controller
import services.blobs.AccessPolicy
import java.text.SimpleDateFormat
import services.http.{EgraphsSession, ControllerMethod}
import play.templates.Html
import services.http.SafePlayParams.Conversions.paramsToOptionalParams
import services.graphics.Handwriting
import models._
import controllers.WebsiteControllers
import frontend.egraph.{LandscapeEgraphFrameViewModel, PortraitEgraphFrameViewModel}
import play.mvc.results.Redirect
import services.AppConfig

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
  def getEgraph(orderId: String) = controllerMethod() {
    // TODO: disable pen-width and shadow re-setting once we find a good value.
    val params = request.params
    val penWidth = params.getOption("penWidth").getOrElse("5.0").toDouble
    val shadowX = params.getOption("shadowX").getOrElse("3.0").toDouble
    val shadowY = params.getOption("shadowY").getOrElse("3.0").toDouble

    // Get an order with provided ID
    orderStore.findFulfilledWithId(orderId.toLong) match {
      case Some(FulfilledOrder(order, egraph)) if isViewable(order) =>
        val maybeCustomerId = session.getLongOption(WebsiteControllers.customerIdKey)
        val maybeGalleryLink = maybeCustomerId.map { id =>
          reverse(WebsiteControllers.getCustomerGalleryById(id)).url
        }

        GetEgraphEndpoint.html(
          egraph = egraph,
          order = order,
          penWidth = penWidth,
          shadowX = shadowX,
          shadowY = shadowY,
          facebookAppId = facebookAppId,
          galleryLink = maybeGalleryLink
        )
      case Some(FulfilledOrder(order, egraph)) => Forbidden("This Egraph is private.")
      case None => NotFound("No Egraph exists with the provided identifier.")
    }
  }

  /** Redirects the old egraph url /egraph/{orderId} to the current url */
  def getEgraphRedirect(orderId: String) = {
    new Redirect(reverse(getEgraph(orderId)).url)
  }

  //
  // Other public members
  //
  def lookupGetEgraph(orderId: Long) = {
    reverse(this.getEgraph(orderId.toString))
  }

  private def isViewable(order: Order): Boolean = {
    val customerIdOption = session.getLongOption(WebsiteControllers.customerIdKey)
    val adminIdOption = session.getLongOption(WebsiteControllers.adminIdKey)

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
    val recipient = order.recipient

    // Prepare the framed image
    val frame = product.frame match {
      case PortraitEgraphFrame => PortraitEgraphFrameViewModel
      case LandscapeEgraphFrame => LandscapeEgraphFrameViewModel
    }

    val rawSignedImage = egraph.image(product.photoImage)
    //TODO w1zzle handle this (see if its worth removing the signing origin transform)
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

    // TODO refactor with social-related code in Order
    // Social links
    val celebName = celebrity.publicName.get
    val thisPageAction = WebsiteControllers.reverse(WebsiteControllers.getEgraph(order.id.toString))
    thisPageAction.absolute()
    val thisPageLink = thisPageAction.url

    val shareOnFacebookLink = views.frontend.Utils.getFacebookShareLink(
      appId = facebookAppId,
      picUrl = rasterImageUrl,
      name = celebName + " egraph for " + recipient.name,
      caption = "Created by " + celebName + " on " + formattedSigningDate,
      description = "",
      link = thisPageLink
    )

    val tweetTextIfCelebHasTwitterName = celebrity.twitterUsername.map { celebTwitterName =>
      "Hey @" + celebTwitterName + " this is one choice egraph you made."
    }
    val tweetText = tweetTextIfCelebHasTwitterName.getOrElse {
      "Check out this choice egraph from " + celebName + "."
    }

    // Render
    views.frontend.html.egraph(
      signerName = celebrity.publicName.getOrElse("Anony mouse"),
      recipientName = order.redactedRecipientName,
      frameCssClass = frame.cssClass,
      frameLayoutColumns = frame.cssFrameColumnClasses,
      productIconUrl = frameFittedIconUrl,
      storyLayoutColumns = frame.cssStoryColumnClasses,
      storyTitle = story.title,
      storyBody = story.body,
      audioUrl = egraph.assets.audioMp3Url,
      signedImageUrl = svgzImageUrl,
      signedOnDate = formattedSigningDate,
      shareOnFacebookLink = shareOnFacebookLink,
      shareOnTwitterLink = views.frontend.Utils.getTwitterShareLink(thisPageLink, tweetText),
      galleryLink = galleryLink
    )
  }

}