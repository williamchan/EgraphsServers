package controllers.website

import play.mvc.Controller
import services.blobs.AccessPolicy
import java.text.SimpleDateFormat
import services.http.ControllerMethod
import models.{Order, Egraph, OrderStore, FulfilledOrder}
import play.templates.Html
import services.http.OptionParams.Conversions.paramsToOptionalParams
import services.graphics.Handwriting

private[controllers] trait GetEgraphEndpoint { this: Controller =>
  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod

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
      case Some(FulfilledOrder(order, egraph)) =>
        GetEgraphEndpoint.html(
          egraph = egraph,
          order = order,
          penWidth = penWidth,
          shadowX = shadowX,
          shadowY = shadowY
        )

      case None =>
        NotFound("No Egraph exists with the provided identifier.")
    }
  }

  def lookupGetEgraph(orderId: Long) = {
    reverse(this.getEgraph(orderId.toString))
  }
}

object GetEgraphEndpoint {

  def html(
    egraph: Egraph,
    order: Order,
    penWidth: Double=Handwriting.defaultPenWidth,
    shadowX: Double=Handwriting.defaultShadowOffsetX,
    shadowY: Double=Handwriting.defaultShadowOffsetY
  ): Html = {
    // Get related data model objects
    val product = order.product
    val celebrity = product.celebrity

    // Prepare the framed image
    val frame = product.frame
    val rawSignedImage = egraph.image(product.photoImage)
    val frameFittedImage = rawSignedImage
      .withPenWidth(penWidth)
      .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
      .withPenShadowOffset(shadowX, shadowY)
      .scaledToWidth(frame.imageWidthPixels)

    // TODO: change this saveAndGetUrl to getSavedUrl when we're comfortable enough with image quality to cache them permanently.
    val frameFittedImageUrl = frameFittedImage.saveAndGetUrl(AccessPolicy.Public)

    // Prepare the icon
    val icon = product.icon
    val frameFittedIconUrl = icon.resized(41, 41).getSaved(AccessPolicy.Public).url

    // Prepare the story
    val story = egraph.story(celebrity, product, order)

    // Render
    views.Application.html.egraph(
      signerName = celebrity.publicName.getOrElse("Anony mouse"),
      recipientName = order.recipientName,
      frameCssClass = frame.cssClass,
      frameLayoutColumns = frame.cssFrameColumnClasses,
      productIconUrl = frameFittedIconUrl,
      storyLayoutColumns = frame.cssStoryColumnClasses,
      storyTitle = story.title,
      storyBody = story.body,
      audioUrl = egraph.assets.audioUrl,
      signedImageUrl = frameFittedImageUrl,
      signedOnDate = new SimpleDateFormat("MMMM dd, yyyy").format(egraph.created)
    )
  }

}