package services.mvc

import models.{Product, Egraph, Order}
import models.frontend.storefront.ChoosePhotoRecentEgraph
import services.blobs.AccessPolicy
import controllers.WebsiteControllers

/**
 * Implicit conversions to turn [[models.Order]]s into related view models.
 */
object OrderViewConversions {

  /**
   * Turns an order and associated product and egraph into a ChoosePhotoRecentEgraph,
   * which is used on the Choose Photo page.
   **/
  def productOrderAndEgraphToChoosePhotoRecentEgraph(product: Product, order: Order, egraph: Egraph)
  : ChoosePhotoRecentEgraph =
  {
    ChoosePhotoRecentEgraph(
      productTitle=product.name,
      ownersName=order.recipientName,
      imageUrl=egraph.image(product.photoImage).rasterized.scaledToWidth(340).getSavedUrl(AccessPolicy.Public),
      url=WebsiteControllers.lookupGetEgraph(order.id).url
    )
  }
}