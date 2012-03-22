package controllers.website

import play.mvc.Controller
import models.{OrderStore, FulfilledOrder}
import services.blobs.AccessPolicy
import java.text.SimpleDateFormat

private[controllers] trait GetEgraphEndpoint { this: Controller =>
  protected def orderStore: OrderStore

  /**
   * Serves up a single egraph HTML page. The egraph number is actually the number
   * of the associated order, as several attempts to satisfy an egraph could have
   * been made before a successful one was signed.
   */
  def getEgraph(orderId: String) = {
    // Get an order with provided ID
    orderStore.findFulfilledWithId(orderId.toLong) match {
      case Some(FulfilledOrder(order, egraph)) =>
        // Get related data model objects
        val product = order.product
        val celebrity = product.celebrity

        // Prepare the framed image
        val frame = product.frame
        val rawSignedImage = egraph.assets.image
        val frameFittedImage = rawSignedImage.resized(frame.imageWidthPixels, frame.imageHeightPixels)
        val frameFittedImageUrl = frameFittedImage.getSaved(AccessPolicy.Public).url

        // Prepare the story
        val story = egraph.story(celebrity, product, order)

        // Render
        views.Application.html.egraph(
          signerName=celebrity.publicName.getOrElse("Anony mouse"),
          recipientName=order.recipientName,
          frameCssClass=frame.cssClass,
          frameLayoutColumns=frame.cssFrameColumnClasses,
          productIconUrl=product.iconUrl,
          storyLayoutColumns=frame.cssStoryColumnClasses,
          storyTitle=story.title,
          storyBody=story.body,
          audioUrl=egraph.assets.audioUrl,
          signedImageUrl=frameFittedImageUrl,
          signedOnDate=new SimpleDateFormat("MMMM dd, yyyy").format(egraph.created)
        )

      case None =>
        NotFound("No Egraph exists with the provided identifier.")
    }
  }

  def lookupGetEgraph(orderId: Long) = {
    reverse(this.getEgraph(orderId.toString))
  }
}
