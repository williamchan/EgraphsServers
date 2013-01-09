package services.mvc

import models._
import com.google.inject.Inject
import controllers.routes.WebsiteControllers.getFAQ
import frontend.storefront.OrderCompleteViewModel
import org.joda.money.{CurrencyUnit, Money}
import services.config.ConfigFileProxy

/**
 * Creates OrderCompleteViewModels for rendering the Order Complete page
 * from domain models.
 *
 * see celebrity_storefront_complete.scala.html
 */
class OrderCompleteViewModelFactory @Inject()(config: ConfigFileProxy) {
  /**
   * Creates the ViewModel from a single Order, accessing necessary extra
   * data from the database.
   **/
  def fromOrder(order: Order): OrderCompleteViewModel = {
    val product = order.product
    val buyer = order.buyer
    val cashTransaction = order.services.cashTransactionStore.findByOrderId(order.id).headOption
    val maybePrintOrder = order.services.printOrderStore.findByOrderId(order.id).headOption

    this.fromModels(
      product.celebrity,
      product,
      buyer,
      buyer.account,
      order.recipient.account,
      order,
      order.inventoryBatch,
      cashTransaction,
      maybePrintOrder
    )
  }

  //
  // Private members
  //
  /**
   * Creates the ViewModel from the lowest-level domain models.
   *
   * This method requires no database access (or shouldn't)
   *
   * @param celebrity the celebrity who owns the product that was bought
   * @param product the product that was bought
   * @param buyer the customer who bought the product
   * @param buyerAccount the account of the customer who brought the product
   * @param recipientAccount the account of the customer who will receive the product
   * @param order the order that represented the egraph purchase
   * @param inventoryBatch the inventory batch against which the order was made.
   * @param cashTransaction the associated cash transaction, if available.
   * @param maybePrintOrder a PrintOrder associated this purchase, if available.
   *
   * @return a ViewModel that populates the order complete page.
   */
  private def fromModels(
    celebrity: Celebrity,
    product: Product,
    buyer: Customer,
    buyerAccount: Account,
    recipientAccount: Account,
    order: Order,
    inventoryBatch: InventoryBatch,
    cashTransaction: Option[CashTransaction],
    maybePrintOrder: Option[PrintOrder]
  ): OrderCompleteViewModel =
  {
    val faqHowLongLink = getFAQ.url + "#how-long"
    val totalAmountPaid = cashTransaction.map(_.cash).getOrElse(Money.zero(CurrencyUnit.USD))
    val isLiveConsumerSite = (config.applicationBaseUrl == "https://www.egraphs.com/")
    val printPrice = maybePrintOrder.map(_.amountPaid).getOrElse(Money.zero(CurrencyUnit.USD))
    
    OrderCompleteViewModel (
      orderDate = order.created,
      orderNumber = order.id,
      buyerName = buyer.name,
      buyerEmail = buyerAccount.email,
      ownerName = order.recipientName,
      ownerEmail = recipientAccount.email,
      celebName = celebrity.publicName,
      productName = product.name,
      productId = product.id,
      expectedDeliveryDate = Order.expectedDeliveryDate(celebrity),
      faqHowLongLink = faqHowLongLink,
      totalPrice = totalAmountPaid,
      discount = None,
      digitalPrice = product.price,
      printPrice = printPrice,
      hasPrintOrder = maybePrintOrder.isDefined,
      withAffiliateMarketing = isLiveConsumerSite
    )
  }
}
