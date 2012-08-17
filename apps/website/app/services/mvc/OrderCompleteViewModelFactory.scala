package services.mvc

import models.frontend.storefront.OrderCompleteViewModel
import models.{InventoryBatch, Product, Account, Customer, Celebrity, Order}
import com.google.inject.Inject

/**
 * Creates OrderCompleteViewModels for rendering the Order Complete page
 * from domain models.
 *
 * see celebrity_storefront_complete.scala.html
 */
class OrderCompleteViewModelFactory @Inject()() {
  /**
   * Creates the ViewModel from a single Order, accessing necessary extra
   * data from the database.
   **/
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

  /**
   * Creates the ViewModel from the lowest-level domain models.
   *
   * This method requires no database access (or shouldn't)
   *
   * @param celeb the celebrity who owns the product that was bought
   * @param product the product that was bought
   * @param buyer the customer who bought the product
   * @param buyerAccount the account of the customer who brought the product
   * @param recipientAccount the account of the customer who will receive the product
   * @param order the order that represented the egraph purchase
   * @param inventoryBatch the inventory batch against which the order was made.
   *
   * @return a ViewModel that populates the order complete page.
   */
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
