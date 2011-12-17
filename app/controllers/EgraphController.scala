package controllers

import play.mvc.Controller
import models.{Egraph, FulfilledOrder, Order}
import libs.{Utils, Blobs}


object EgraphController extends Controller
  with DBTransaction
{

  def egraph(orderId: String) = {
    // Get an order with provided ID
    Order.findFulfilledWithId(orderId.toLong) match {
      case Some(FulfilledOrder(order, egraph)) =>
        val imageUrl = egraph.assets.image.resizedWidth(460).getSaved(Blobs.AccessPolicy.Public).url
        val product = order.product
        val celebrity = product.celebrity
        val recipient = order.recipient

        views.Application.html.egraph(
          order,
          egraph,
          imageUrl,
          product,
          celebrity,
          recipient)

      case None =>
        NotFound("No eGraph exists with the provided identifier.")
    }
  }

  def lookup(order: Order) = {
    reverse(this.egraph(order.id.toString))
  }
}