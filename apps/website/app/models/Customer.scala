package models

import com.google.inject.{Provider, Inject}
import enums.OrderReviewStatus
import exception.InsufficientInventoryException
import java.sql.Timestamp
import org.apache.commons.mail.HtmlEmail
import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.config.ConfigFileProxy
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import services.mail._
import services.{Time, AppConfig}

/** Services used by each instance of Customer */
case class CustomerServices @Inject() (
  accountStore: AccountStore,
  customerStore: CustomerStore,
  inventoryBatchStore: InventoryBatchStore,
  usernameHistoryStore: UsernameHistoryStore,
  mail: TransactionalMail,
  config: ConfigFileProxy
)

/**
 * Persistent entity representing customers who buy products from our service.
 */
case class Customer(
  id: Long = 0L,
  name: String = "",
  username: String = "",
  isGalleryVisible: Boolean = true,
  notice_stars: Boolean = false,
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
    require(!username.isEmpty, "Customer: username must be specified")
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
          requestedMessage: Option[String] = None): Either[Exception, Order] = {

    for {
      inventoryBatch <- product.availableInventoryBatches.headOption.toRight(new InsufficientInventoryException("Must have available inventory to purchase product " + product.id)).right
    } yield {
      val order = Order(
        buyerId = id,
        recipientId = recipient.id,
        productId = product.id,
        amountPaidInCurrency = BigDecimal(product.price.getAmount),
        recipientName = recipientName,
        messageToCelebrity = messageToCelebrity,
        requestedMessage = requestedMessage,
        inventoryBatchId = inventoryBatch.id,
        expectedDate = Order.expectedDeliveryDate(product.celebrity)
      )

      // If admin review is turned off (eg to expedite demos), create the Order already approved
      if (services.config.adminreviewSkip) {
        order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin)
      } else {
        order
      }
    }
  }

  /**
   * This alternative of buy will just throw the exceptions found in the either from buy.
   */
  def buyUnsafe(product: Product,
          recipient: Customer = this,
          recipientName: String = this.name,
          messageToCelebrity: Option[String] = None,
          requestedMessage: Option[String] = None): Order = {
    buy(product, recipient, recipientName, messageToCelebrity, requestedMessage).fold(
      error => throw error,
      order => order
    )
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Customer.unapply(this)
  }
}

case class JsCustomer(id: Long, name: String)

object JsCustomer {
  implicit val customerWrites = Json.writes[JsCustomer]

  def from(customer: Customer): JsCustomer = {
    JsCustomer(id = customer.id, name = customer.name)
  }
}

class CustomerStore @Inject() (
  schema: Schema,
  accountStore: AccountStore,
  customerServices: Provider[CustomerServices],
  accountServices: Provider[AccountServices]
) extends SavesWithLongKey[Customer] with SavesCreatedUpdated[Customer]
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
    findByEmail(email).getOrElse(createByEmail(email, name))
  }

  def findOrCreateByEmail(email: String): Customer = findOrCreateByEmail(email, email takeWhile (_ != '@'))

  def createByEmail(email: String, name: String): Customer = {
    val accountOption = accountStore.findByEmail(email)
    val account = accountOption.getOrElse(Account(email = email, _services = accountServices.get))
    val unsavedUsernameHistory = account.createUsername()

    val customer = account.customerId match {
      case Some(id) => get(id)
      case None => {
        val newCust = account.createCustomer(name).save()
        account.copy(customerId = Some(newCust.id)).save()
        newCust
      }
    }
    unsavedUsernameHistory.copy(customerId = customer.id).save()

    customer
  }

  def findByEmail(email: String) = {
    join (table, schema.accounts) ( (customer, account) =>
      where (lower(account.email) === lower(email))
      select (customer) on (customer.id === account.customerId)
    ).headOption
  }

  def findByUsername(username: String): Option[Customer] = {
    //TODO: SER-223 (in progress) change this to instead use UsernameHistoryStore when that is ready
    from(schema.customers)((customer) => where(lower(customer.username) === username.toLowerCase) select (customer)).headOption
  }

  //
  // SavesWithLongKey[Customer] methods
  //
  override val table = schema.customers



  //
  // SavesCreatedUpdated[Customer] methods
  //
  override def withCreatedUpdated(toUpdate: Customer, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}