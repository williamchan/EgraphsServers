package services.mvc

import models.frontend.storefront.OrderCompleteViewModel
import models.{InventoryBatch, Product, Account, Customer, Celebrity, Order}
import com.google.inject.Inject

class OrderCompleteViewModelFactory @Inject()() {
  def fromOrder(order: Order): OrderCompleteViewModel = {
    val product = order.product
    val buyer = order.buyer

    this.fromModels(
      product.celebrity,
      product,
      buyer,
      buyer.account,
      order.recipient.account,
      order,
      order.inventoryBatch
    )
  }

  def fromModels(
    celeb: Celebrity,
    product: Product,
    buyer: Customer,
    buyerAccount: Account,
    recipientAccount: Account,
    order: Order,
    inventoryBatch: InventoryBatch
  ): OrderCompleteViewModel =
  {
    OrderCompleteViewModel (
      orderDate = order.created,
      orderNumber = order.id,
      buyerName = buyer.name,
      buyerEmail = buyerAccount.email,
      ownerName = order.recipientName,
      ownerEmail = recipientAccount.email,
      celebName = celeb.publicName,
      productName = product.name,
      totalPrice = order.amountPaid,
      guaranteedDeliveryDate = inventoryBatch.getExpectedDate
    )
  }
}
