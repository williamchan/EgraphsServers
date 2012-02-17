package controllers.website

import play.mvc.Controller
import models.{Order, OrderStore, FulfilledOrder}
import services.blobs.{AccessPolicy, Blobs}

private[controllers] trait GetEgraphEndpoint { this: Controller =>
  protected def orderStore: OrderStore

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
        NotFound("No eGraph exists with the provided identifier.")
    }
  }

  def lookupGetEgraph(orderId: Long) = {
    reverse(this.getEgraph(orderId.toString))
  }
}
