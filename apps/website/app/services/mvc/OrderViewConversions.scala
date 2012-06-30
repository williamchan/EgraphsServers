package services.mvc

import models.{Product, Egraph, Order}
import models.frontend.storefront.ChoosePhotoRecentEgraph
import services.blobs.AccessPolicy
import controllers.WebsiteControllers

object OrderViewConversions {

  def productOrderAndEgraphToChoosePhotoRecentEgraph(product: Product, order: Order, egraph: Egraph)
  : ChoosePhotoRecentEgraph =
  {
    ChoosePhotoRecentEgraph(
      productTitle=product.name,
      ownersName=order.recipientName,
      imageUrl=egraph.image(product.photoImage).scaledToWidth(340).getSavedUrl(AccessPolicy.Public),
      url=WebsiteControllers.lookupGetEgraph(order.id).url
    )
  }
}