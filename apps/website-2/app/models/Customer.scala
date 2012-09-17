package models

import enums.OrderReviewStatus
import java.sql.Timestamp
import services.{Utils, Time, AppConfig}
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import com.google.inject.{Provider, Inject}
import exception.InsufficientInventoryException
import org.apache.commons.mail.HtmlEmail
import services.mail.TransactionalMail
import services.http.PlayConfig
import java.util.Properties

/** Services used by each instance of Customer */
case class CustomerServices @Inject() (accountStore: AccountStore,
                                       customerStore: CustomerStore,
                                       inventoryBatchStore: InventoryBatchStore,
                                       usernameHistoryStore: UsernameHistoryStore,
                                       mail: TransactionalMail,
                                       @PlayConfig playConfig: Properties)

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
          requestedMessage: Option[String] = None): Order = {

    val (remainingInventory, activeInventoryBatches) = product.getRemainingInventoryAndActiveInventoryBatches()
    if (remainingInventory <= 0 || activeInventoryBatches.headOption.isEmpty) throw new InsufficientInventoryException("Must have available inventory to purchase product " + product.id)

    val batchToOrderAgainst = services.inventoryBatchStore.selectAvailableInventoryBatch(activeInventoryBatches)
    val (inventoryBatchId, expectedDate) = batchToOrderAgainst match {
      case Some(b) => (b.id, Option(b.getExpectedDate))
      case _ => (0L, None) // todo(wchan): Do we want to permit the order to go through?
    }

    val order = Order(
      buyerId=id,
      recipientId=recipient.id,
      productId=product.id,
      amountPaidInCurrency=BigDecimal(product.price.getAmount),
      recipientName = recipientName,
      messageToCelebrity = messageToCelebrity,
      requestedMessage = requestedMessage,
      inventoryBatchId = inventoryBatchId,
      expectedDate = expectedDate
    )

    // If admin review is turned off (eg to expedite demos), create the Order already approved
    services.playConfig.getProperty("adminreview.skip") match {
      case "true" => order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin)
      case _ => order
    }
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Customer.unapply(this)
  }
}

object Customer {
  def sendNewCustomerEmail(account: Account, verificationNeeded: Boolean = false, mail: TransactionalMail) {
    val email = new HtmlEmail()
    email.setFrom("noreply@egraphs.com")
    email.addReplyTo("noreply@egraphs.com")
    email.addTo(account.email)
    email.setSubject("Welcome to Egraphs!")
    //    val emailLogoSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-logo.jpg")))
    //    val emailFacebookSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-facebook.jpg")))
    //    val emailTwitterSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-twitter.jpg")))
    val emailLogoSrc = ""
    val emailFacebookSrc = ""
    val emailTwitterSrc = ""

    if (verificationNeeded) {
      val verifyPasswordAction = Utils.lookupUrl("WebsiteControllers.getVerifyAccount",
        Map("email" -> account.email, "secretKey" -> account.resetPasswordKey.get))
      val verifyPasswordUrl = Utils.absoluteUrl(verifyPasswordAction)
      email.setHtmlMsg(views.html.frontend.email_account_verification(
        verifyPasswordUrl = verifyPasswordUrl,
        emailLogoSrc = emailLogoSrc,
        emailFacebookSrc = emailFacebookSrc,
        emailTwitterSrc = emailTwitterSrc
      ).toString()
      )
      email.setTextMsg(views.html.frontend.email_account_verification_text(verifyPasswordUrl).toString())
    } else {
      email.setHtmlMsg(views.html.frontend.email_account_confirmation(
        emailLogoSrc = emailLogoSrc,
        emailFacebookSrc = emailFacebookSrc,
        emailTwitterSrc = emailTwitterSrc
      ).toString()
      )
      email.setTextMsg(views.html.frontend.email_account_confirmation_text.toString())
    }

    mail.send(email)
  }

}

class CustomerStore @Inject() (
  schema: Schema,
  accountStore: AccountStore,
  customerServices: Provider[CustomerServices],
  accountServices: Provider[AccountServices]
) extends SavesWithLongKey[Customer] with SavesCreatedUpdated[Long,Customer]
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
      case Some(customer) => customer

      // Customer face didn't exist. Use existing or new Account to create
      // the face and return it.
      case None =>
        val account = accountOption.getOrElse(Account(email=email, services=accountServices.get))
        val unsavedCustomer = account.createCustomer(name)
        val unsavedUsernameHistory = account.createUsername()
        val customer = unsavedCustomer.save()
        account.copy(customerId=Some(customer.id)).save()
        val usernameHistory = unsavedUsernameHistory.copy(customerId=customer.id).save()
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

  override def defineUpdate(theOld: Customer, theNew: Customer) = {
    updateIs(
      theOld.name  := theNew.name,
      theOld.username  := theNew.username,
      theOld.isGalleryVisible  := theNew.isGalleryVisible,
      theOld.notice_stars := theNew.notice_stars,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,Customer] methods
  //
  override def withCreatedUpdated(toUpdate: Customer, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}