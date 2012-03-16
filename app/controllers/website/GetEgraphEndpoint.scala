package controllers.website

import play.mvc.Controller
import models.{OrderStore, FulfilledOrder}
import services.blobs.AccessPolicy

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
        val imageUrl = egraph.assets.image.resizedWidth(940).getSaved(AccessPolicy.Public).url
        val product = order.product
        val celebrity = product.celebrity

        views.Application.html.egraph(
          order,
          egraph,
          imageUrl,
          product,
          celebrity
        )

      case None =>
        NotFound("No Egraph exists with the provided identifier.")
    }
  }

  def lookupGetEgraph(orderId: Long) = {
    reverse(this.getEgraph(orderId.toString))
  }
}
