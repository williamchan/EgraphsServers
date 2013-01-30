package models

import enums.OrderReviewStatus
import java.sql.Timestamp
import services.{Time, AppConfig}
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import com.google.inject.{Provider, Inject}
import exception.InsufficientInventoryException
import org.apache.commons.mail.HtmlEmail
import services.mail.TransactionalMail
import play.api.mvc.RequestHeader
import controllers.routes.WebsiteControllers.getVerifyAccount
import play.api.templates.Html
import services.ConsumerApplication
import services.config.ConfigFileProxy

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

  def renderedForApi: Map[String, Any] = {
    Map("id" -> id, "name" -> name)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Customer.unapply(this)
  }
}

object Customer {
  def sendNewCustomerEmail(
      account: Account, 
      verificationNeeded: Boolean = false, 
      mail: TransactionalMail, 
      consumerApp: ConsumerApplication
  )(implicit request: RequestHeader)
  {
    val email = new HtmlEmail()
    email.setFrom("webserver@egraphs.com")
    email.addReplyTo("webserver@egraphs.com")
    email.addTo(account.email)
    email.setSubject("Welcome to Egraphs!")

    val (textMsg: String, htmlMsg: Html) = if (verificationNeeded) {
      val verifyPasswordUrl = consumerApp.absoluteUrl(getVerifyAccount(account.email, account.resetPasswordKey.get).url)
      val html = views.html.frontend.email_account_verification(verifyPasswordUrl = verifyPasswordUrl)
      val text = views.html.frontend.email_account_verification_text(verifyPasswordUrl).toString
      (text, html)
    } else {
      val html = views.html.frontend.email_account_confirmation()
      val text = views.html.frontend.email_account_confirmation_text.toString
      (text, html)
    }

    mail.send(email, Some(textMsg), Some(htmlMsg))
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

  def createByEmail(email: String, name: String): Customer = {
    val accountOption = accountStore.findByEmail(email)
    val account = accountOption.getOrElse(Account(email = email, services = accountServices.get))
    val unsavedCustomer = account.createCustomer(name)
    val unsavedUsernameHistory = account.createUsername()
    val customer = unsavedCustomer.save()
    account.copy(customerId = Some(customer.id)).save()
    val usernameHistory = unsavedUsernameHistory.copy(customerId = customer.id).save()

    customer
  }
  
  def findByEmail(email: String): Option[Customer] = {
    // TODO: Optimize this using a single outer-join query to get Customer + Account all at once

    // Get the Account and Customer face if both exist.
    for {
      account <- accountStore.findByEmail(email)
      customerId <- account.customerId
      customer <- findById(customerId)
    } yield {
      customer
    }
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