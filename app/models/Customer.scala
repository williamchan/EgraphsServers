package models

import java.sql.Timestamp
import services.Time
import services.db.{KeyedCaseClass, Schema, Saves}
import services.AppConfig
import com.google.inject.{Provider, Inject}
import exception.InsufficientInventoryException

/** Services used by each instance of Customer */
case class CustomerServices @Inject() (accountStore: AccountStore, customerStore: CustomerStore, inventoryBatchStore: InventoryBatchStore)

/**
 * Persistent entity representing customers who buy products from our service.
 */
case class Customer(
  id: Long = 0L,
  name: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CustomerServices = AppConfig.instance[CustomerServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public methods
  //
  def save(): Customer = {
    require(!name.isEmpty, "Customer: name must be specified")
    services.customerStore.save(this)
  }

  /** Retrieves the Customer's Account from the database */
  def account: Account = {
    services.accountStore.findByCustomerId(id).get
  }

  /**
   * Orders an Egraph product on behalf of a recipient. The results must be persisted for the transaction
   * to actually accomplish anything.
   *
   * @param product the Product to buy
   * @param recipient the Customer receiving the Egraph. Defaults to the purchasing Customer.
   *
   * @return a tuple of the Order for the Product purchased by this Customer for the recipient Customer
   *   and the transaction that took place.
   */
  def buy(product: Product,
          recipient: Customer = this,
          recipientName: String = this.name,
          messageToCelebrity: Option[String] = None,
          requestedMessage: Option[String] = None): Order = {

    val (remainingInventory, activeInventoryBatches) = product.getRemainingInventoryAndActiveInventoryBatches()
    if (remainingInventory <= 0 || activeInventoryBatches.headOption.isEmpty) throw new InsufficientInventoryException("Must have available inventory to purchase product " + product.id)

    val batchToOrderAgainst = services.inventoryBatchStore.selectAvailableInventoryBatch(activeInventoryBatches)
    val inventoryBatchId = batchToOrderAgainst match {
      case Some(b) => b.id
      case _ => 0L // todo(wchan): Do we want to permit the order to go through?
    }

    Order(
      buyerId=id,
      recipientId=recipient.id,
      productId=product.id,
      amountPaidInCurrency=BigDecimal(product.price.getAmount),
      recipientName = recipientName,
      messageToCelebrity = messageToCelebrity,
      requestedMessage = requestedMessage,
      inventoryBatchId = inventoryBatchId
    )
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Customer.unapply(this)
  }
}

class CustomerStore @Inject() (
  schema: Schema,
  accountStore: AccountStore,
  customerServices: Provider[CustomerServices],
  accountServices: Provider[AccountServices]
) extends Saves[Customer] with SavesCreatedUpdated[Customer]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public members
  //

  /**
   * Either retrieves an existing Customer keyed by the provided e-mail address
   * or creates one, saves it to the database, then returns it.
   *
   * @param email the address of the Customer to look up or create
   * @param name name of the Customer. Used only if the Customer does not already exist.
   *
   * @return a persisted Customer with a valid ID.
   */
  def findOrCreateByEmail(email: String, name: String): Customer = {
    // TODO: Optimize this using a single outer-join query to get Customer + Account all at once

    // Get the Account and Customer face if both exist.
    val accountOption = accountStore.findByEmail(email)
    val customerOption = accountOption.flatMap { account =>
      account.customerId.flatMap { customerId =>
        findById(customerId)
      }
    }

    customerOption match {
      // Customer already existed
      case Some(customer) =>
        customer

      // Customer face didn't exist. Use existing or new Account to create
      // the face and return it.
      case None =>
        val customer = Customer(name=name, services=customerServices.get).save()
        val account = accountOption.getOrElse(Account(email=email, services=accountServices.get))

        account.copy(customerId=Some(customer.id)).save()

        customer
    }
  }

  //
  // Saves[Customer] methods
  //
  override val table = schema.customers

  override def defineUpdate(theOld: Customer, theNew: Customer) = {
    updateIs(
      theOld.name  := theNew.name,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Customer] methods
  //
  override def withCreatedUpdated(toUpdate: Customer, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}