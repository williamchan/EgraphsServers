package services.db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Table}
import models._
import models.vbg._
import models.xyzmo._
import java.lang.IllegalStateException
import java.io.{ByteArrayOutputStream, PrintWriter}
import com.google.inject.{Inject, Injector}
import java.sql.Connection
import services.logging.Logging
import play.api.Configuration

/**
 * Egraphs Database schema
 *
 * When inspecting the schema of a database table, inspect both this object and the KeyedCaseClass.
 *
 * NOTE: The order of declaration matters. Cannot reference tables before they have been declared.
 */
class Schema @Inject()(
  injector: Injector,
  playConfig: Configuration,
  @CurrentTransaction currentTxnConnectionFactory: () => Connection
) extends org.squeryl.Schema with Logging
{

  import uk.me.lings.scalaguice.InjectorExtensions._

  //
  // table declarations -- please keep these alphabetized
  //

  val accounts = table[Account]
  on(accounts)(account => declare(account.email is unique))

  val addresses = table[Address]
  on(addresses)(address =>
    declare(
      address._state is dbType("varchar(2)"),
      address.postalCode is dbType("varchar(20)")
    )
  )

  val administrators = table[Administrator]

  val blobKeys = table[BlobKey]
  on(blobKeys)(blobKey =>
    declare(
      blobKey.key is unique,
      blobKey.url is dbType("varchar(255)")
    )
  )

  val cashTransactions = table[CashTransaction]
  on(cashTransactions)(cashTransaction => declare(
    cashTransaction.amountInCurrency is monetaryDbType,
    cashTransaction.billingPostalCode is dbType("varchar(20)"))
  )

  val celebrities = table[Celebrity]
  on(celebrities)(celebrity =>
    declare(
      celebrity.urlSlug is unique,
      celebrity.isFeatured is indexed,
      celebrity.bio is dbType("text")
    )
  )

  val customers = table[Customer]
  on(customers)(customer => declare(customer.username is unique))

  val egraphs = table[Egraph]
  on(egraphs)(egraph => declare(columns(egraph.orderId, egraph._egraphState) are indexed))

  val enrollmentBatches = table[EnrollmentBatch]
  on(enrollmentBatches)(enrollmentBatch =>
    declare(
      columns(enrollmentBatch.celebrityId, enrollmentBatch.isBatchComplete, enrollmentBatch.isSuccessfulEnrollment) are indexed
    )
  )

  val enrollmentSamples = table[EnrollmentSample]

  val failedPurchaseData = table[FailedPurchaseData]
  on(failedPurchaseData)(datum => declare( datum.purchaseData is dbType("varchar(1000)") ))

  val inventoryBatches = table[InventoryBatch]
  on(inventoryBatches)(inventoryBatch =>
    declare(
      columns(inventoryBatch.startDate, inventoryBatch.endDate) are indexed,
      columns(inventoryBatch.celebrityId, inventoryBatch.startDate, inventoryBatch.endDate) are indexed
    )
  )

  val orders = table[Order]("Orders")
  on(orders)(order =>
    declare(
      order.amountPaidInCurrency is monetaryDbType,
      columns(order._reviewStatus) are indexed,
      order.billingPostalCode is dbType("varchar(20)"),
      order.shippingAddress is dbType("varchar(255)"),
      order.messageToCelebrity is dbType("varchar(140)")
    )
  )

  val printOrders = table[PrintOrder]
  on(printOrders)(printOrder => declare(
    printOrder.amountPaidInCurrency is monetaryDbType,
    printOrder.isFulfilled is indexed,
    printOrder.pngUrl is dbType("varchar(255)"),
    printOrder.shippingAddress is dbType("varchar(255)"))
  )

  val products = table[Product]
  on(products)(product =>
    declare(
      product.priceInCurrency is monetaryDbType,
      product.storyText is dbType("text"),
      columns(product.celebrityId, product.urlSlug) are unique
    )
  )

  val usernameHistories = table[Username]("Usernames")
  on(usernameHistories)(usernameHistory =>
    declare(
      usernameHistory.id is dbType("varchar(255)"),
      usernameHistory.customerId is indexed
    )
  )

  // ugh, why did I make so many biometrics tables?
  val vbgAudioCheckTable = table[VBGAudioCheck]
  val vbgEnrollUserTable = table[VBGEnrollUser]
  val vbgFinishEnrollTransactionTable = table[VBGFinishEnrollTransaction]
  val vbgFinishVerifyTransactionTable = table[VBGFinishVerifyTransaction]
  val vbgStartEnrollmentTable = table[VBGStartEnrollment]
  val vbgStartVerificationTable = table[VBGStartVerification]
  val vbgVerifySampleTable = table[VBGVerifySample]
  val xyzmoAddProfileTable = table[XyzmoAddProfile]
  on(xyzmoAddProfileTable)(xyzmoAddProfile => declare(xyzmoAddProfile.errorMsg is dbType("varchar(255)")))
  val xyzmoAddUserTable = table[XyzmoAddUser]
  on(xyzmoAddUserTable)(xyzmoAddUser => declare(xyzmoAddUser.errorMsg is dbType("varchar(255)")))
  val xyzmoDeleteUserTable = table[XyzmoDeleteUser]
  on(xyzmoDeleteUserTable)(xyzmoDeleteUser => declare(xyzmoDeleteUser.errorMsg is dbType("varchar(255)")))
  val xyzmoEnrollDynamicProfileTable = table[XyzmoEnrollDynamicProfile]
  on(xyzmoEnrollDynamicProfileTable)(xyzmoEnrollDynamicProfile =>
    declare(
      xyzmoEnrollDynamicProfile.errorMsg is dbType("varchar(255)"),
      xyzmoEnrollDynamicProfile.rejectedSignaturesSummary is dbType("varchar(255)"),
      xyzmoEnrollDynamicProfile.enrollmentSampleIds is dbType("varchar(255)")
    )
  )
  val xyzmoVerifyUserTable = table[XyzmoVerifyUser]
  on(xyzmoVerifyUserTable)(xyzmoVerifyUser => declare(xyzmoVerifyUser.errorMsg is dbType("varchar(255)")))


  //
  // manyToManyRelation declarations -- please keep these alphabetized
  //

  val inventoryBatchProducts = manyToManyRelation(inventoryBatches, products)
    .via[InventoryBatchProduct]((inventoryBatch, product, join) => (join.inventoryBatchId === inventoryBatch.id, join.productId === product.id))
  on(inventoryBatchProducts)(inventoryBatchProduct =>
    declare(
      columns(inventoryBatchProduct.inventoryBatchId, inventoryBatchProduct.productId) are unique
    )
  )

  //
  // oneToManyRelation declarations -- please keep these organized
  //

  val accountToAdministrator = oneAccountPerRowOn(administrators, (account) => account.administratorId)
  val accountToCelebrity = oneAccountPerRowOn(celebrities, (account) => account.celebrityId)
  val accountToCustomer = oneAccountPerRowOn(customers, (account) => account.customerId)
  val accountToAddress = oneToManyRelation(accounts, addresses)
    .via((account, address) => account.id === address.accountId)
  val accountToTransaction = oneToManyRelation(accounts, cashTransactions)
    .via((account, cashTransaction) => account.id === cashTransaction.accountId)

  val celebrityToEnrollmentBatches = oneToManyRelation(celebrities, enrollmentBatches)
    .via((celebrity, enrollmentBatch) => celebrity.id === enrollmentBatch.celebrityId)
  celebrityToEnrollmentBatches.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val celebrityToProduct = oneToManyRelation(celebrities, products)
    .via((celebrity, product) => celebrity.id === product.celebrityId)
  val celebrityToInventoryBatches = oneToManyRelation(celebrities, inventoryBatches)
    .via((celebrity, inventoryBatch) => celebrity.id === inventoryBatch.celebrityId)

  val customerToUsernameHistory = oneToManyRelation(customers, usernameHistories)
    .via((customer, usernameHistory) => customer.id === usernameHistory.customerId)
  customerToUsernameHistory.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val buyingCustomerToOrders = oneToManyRelation(customers, orders)
    .via((customer, order) => customer.id === order.buyerId)
  buyingCustomerToOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val recipientCustomerToOrders = oneToManyRelation(customers, orders)
    .via((customer, order) => customer.id === order.recipientId)
  recipientCustomerToOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val enrollmentBatchToEnrollmentSamples = oneToManyRelation(enrollmentBatches, enrollmentSamples)
    .via((enrollmentBatch, enrollmentSample) => enrollmentBatch.id === enrollmentSample.enrollmentBatchId)
  enrollmentBatchToEnrollmentSamples.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val inventoryBatchToOrders = oneToManyRelation(inventoryBatches, orders)
    .via((inventoryBatch, order) => inventoryBatch.id === order.inventoryBatchId)

  val orderToEgraphs = oneToManyRelation(orders, egraphs)
    .via((order, egraph) => order.id === egraph.orderId)
  orderToEgraphs.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val orderToPrintOrders = oneToManyRelation(orders, printOrders)
    .via((order, printOrder) => order.id === printOrder.orderId)
  orderToPrintOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val productToOrders = oneToManyRelation(products, orders)
    .via((product, order) => product.id === order.productId)
  productToOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)


  // ugh, why did I make so many biometrics tables?
  val enrollmentBatchToVBGAudioCheckTable = oneToManyRelation(enrollmentBatches, vbgAudioCheckTable)
    .via((enrollmentBatch, vbgAudioCheck) => enrollmentBatch.id === vbgAudioCheck.enrollmentBatchId)
  enrollmentBatchToVBGAudioCheckTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val enrollmentBatchToVBGEnrollUserTable = oneToManyRelation(enrollmentBatches, vbgEnrollUserTable)
    .via((enrollmentBatch, vbgEnrollUser) => enrollmentBatch.id === vbgEnrollUser.enrollmentBatchId)
  enrollmentBatchToVBGEnrollUserTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val enrollmentBatchToVBGFinishEnrollTransactionTable = oneToManyRelation(enrollmentBatches, vbgFinishEnrollTransactionTable)
    .via((enrollmentBatch, vbgFinishEnrollTransaction) => enrollmentBatch.id === vbgFinishEnrollTransaction.enrollmentBatchId)
  enrollmentBatchToVBGFinishEnrollTransactionTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val egraphToVBGFinishVerifyTransactionTable = oneToManyRelation(egraphs, vbgFinishVerifyTransactionTable)
    .via((egraph, vbgFinishVerifyTransaction) => egraph.id === vbgFinishVerifyTransaction.egraphId)
  egraphToVBGFinishVerifyTransactionTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val enrollmentBatchToVBGStartEnrollmentTable = oneToManyRelation(enrollmentBatches, vbgStartEnrollmentTable)
    .via((enrollmentBatch, vbgStartEnrollment) => enrollmentBatch.id === vbgStartEnrollment.enrollmentBatchId)
  enrollmentBatchToVBGStartEnrollmentTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val egraphToVBGStartVerificationTable = oneToManyRelation(egraphs, vbgStartVerificationTable)
    .via((egraph, vbgStartVerification) => egraph.id === vbgStartVerification.egraphId)
  egraphToVBGStartVerificationTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val egraphToVBGVerifySampleTable = oneToManyRelation(egraphs, vbgVerifySampleTable)
    .via((egraph, vbgVerifySample) => egraph.id === vbgVerifySample.egraphId)
  egraphToVBGVerifySampleTable.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val enrollmentBatchToXyzmoAddProfileTable = oneToManyRelation(enrollmentBatches, xyzmoAddProfileTable)
    .via((enrollmentBatch, xyzmoAddProfile) => enrollmentBatch.id === xyzmoAddProfile.enrollmentBatchId)
  enrollmentBatchToXyzmoAddProfileTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val enrollmentBatchToXyzmoAddUserTable = oneToManyRelation(enrollmentBatches, xyzmoAddUserTable)
    .via((enrollmentBatch, xyzmoAddUser) => enrollmentBatch.id === xyzmoAddUser.enrollmentBatchId)
  enrollmentBatchToXyzmoAddUserTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val enrollmentBatchToXyzmoDeleteUserTable = oneToManyRelation(enrollmentBatches, xyzmoDeleteUserTable)
    .via((enrollmentBatch, xyzmoDeleteUser) => enrollmentBatch.id === xyzmoDeleteUser.enrollmentBatchId)
  enrollmentBatchToXyzmoDeleteUserTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val enrollmentBatchToXyzmoEnrollDynamicProfileTable = oneToManyRelation(enrollmentBatches, xyzmoEnrollDynamicProfileTable)
    .via((enrollmentBatch, xyzmoEnrollDynamicProfile) => enrollmentBatch.id === xyzmoEnrollDynamicProfile.enrollmentBatchId)
  enrollmentBatchToXyzmoEnrollDynamicProfileTable.foreignKeyDeclaration.constrainReference(onDelete cascade)
  val egraphToXyzmoVerifyUserTable = oneToManyRelation(egraphs, xyzmoVerifyUserTable)
    .via((egraph, xyzmoVerifyUser) => egraph.id === xyzmoVerifyUser.egraphId)
  egraphToXyzmoVerifyUserTable.foreignKeyDeclaration.constrainReference(onDelete cascade)


  //
  // Public methods
  //
  /**Clears out the schema and recreates it. For God's sake don't do this in production. */
  def scrub() {
    val applicationMode = playConfig.getString("application.mode")
    log("Checking application.mode before scrubbing database. Must be in dev mode. Mode is: " + applicationMode)
    if (applicationMode != Some("dev")) {
      throw new IllegalStateException("Cannot scrub database unless in dev mode")
    }

    playConfig.getString("db.allowscrub") match {
      case Some("yes") =>
        if (isInPlace) {
          dropSchema()
        }
        create

      case _ =>
        throw new IllegalStateException(
          """I'm just not going to scrub the DB unless "db.allowscrub" is
          set to "yes" in application.conf. Sorry if you have a problem with that."""
        )
    }
  }

  /**Drops the public schema of the database. This requires specific syntax for some providers. */
  private def dropSchema() {
    DBAdapter.current match {
      case DBAdapter.postgres =>
        // Postgres-specific syntax makes Squeryl's drop() method not bork.
        val conn = currentTxnConnectionFactory()
        conn.prepareStatement("DROP SCHEMA public CASCADE;").execute()
        conn.prepareStatement("CREATE SCHEMA public AUTHORIZATION postgres").execute()

      case _ =>
        drop
    }
  }

  /**Returns the SQL commands that define this schema as a String. */
  def ddl: String = {
    val outputStream = new ByteArrayOutputStream()
    val printWriter = new PrintWriter(outputStream)
    this.printDdl(printWriter)

    printWriter.flush()

    val ddlString = outputStream.toString("UTF-8")

    printWriter.close()
    outputStream.close()

    ddlString
  }

  /**
   * Returns true if it has reason to believe at least SOME egraphs schema already exists on the
   * main Play connection. False otherwise.
   *
   * In practice, we just try to perform a query and if the query barfs then our schema was not
   * yet installed.
   */
  def isInPlace: Boolean = {
    // Prepare a savepoint on the connection; we'll roll back to this point if the test query fails.
    // This is necessary because postgres will throw a runtime exception on any
    // query issued after a failed query until rollback() gets called.
    val conn = currentTxnConnectionFactory()
    val savepoint = conn.setSavepoint()

    try {
      from(celebrities)(celeb =>
        select(celeb.id)
      ).headOption

      log("Existing egraphs schema was detected")
      true
    }
    catch {
      case e: RuntimeException if e.getMessage.toLowerCase.contains("celebrity") =>
        log("No egraphs schema was detected")
        false

      case otherErrors =>
        throw otherErrors
    }
    finally {
      conn.rollback(savepoint)
    }
  }

  //
  // Private Methods
  //
  /**
   * Declares a 1-to-1 relationship of accounts on another table using a combination
   * by declaring a foreign key on the id field of that table.
   *
   * @param table the table upon which accounts depends
   * @param foreignKey function that provides the field in accounts that maps to
   *   the provided table's id field
   *
   */
  private def oneAccountPerRowOn[T <: KeyedEntity[Long]] (table: Table[T], foreignKey: Account => Option[Long]): OneToManyRelationImpl[T, Account] = {
    val relation = oneToManyRelation(table, accounts)
      .via((row, account) => row.id === foreignKey(account))

    on(accounts)(account => declare(foreignKey(account) is (unique)))
    relation.foreignKeyDeclaration.constrainReference(onDelete setNull)

    relation
  }

  /**
   * Defines a type for monetaryDbType fields.
   *
   * Decision based on conversation at:
   * http://stackoverflow.com/questions/224462/storing-money-in-a-decimal-column-what-precision-and-scale
   */
  private def monetaryDbType = {
    dbType("decimal(21, 6)")
  }

  override def callbacks = {
    Seq(
      factoryFor(accounts) is Account(services = injector.instance[AccountServices]),
      factoryFor(addresses) is Address(services = injector.instance[AddressServices]),
      factoryFor(administrators) is Administrator(services = injector.instance[AdministratorServices]),
      factoryFor(blobKeys) is BlobKey(services = injector.instance[BlobKeyServices]),
      factoryFor(cashTransactions) is CashTransaction(services = injector.instance[CashTransactionServices]),
      factoryFor(celebrities) is Celebrity(services = injector.instance[CelebrityServices]),
      factoryFor(customers) is Customer(services = injector.instance[CustomerServices]),
      factoryFor(egraphs) is Egraph(services = injector.instance[EgraphServices]),
      factoryFor(enrollmentBatches) is EnrollmentBatch(services = injector.instance[EnrollmentBatchServices]),
      factoryFor(enrollmentSamples) is EnrollmentSample(services = injector.instance[EnrollmentSampleServices]),
      factoryFor(failedPurchaseData) is FailedPurchaseData(services = injector.instance[FailedPurchaseDataServices]),
      factoryFor(inventoryBatches) is InventoryBatch(services = injector.instance[InventoryBatchServices]),
      factoryFor(inventoryBatchProducts) is InventoryBatchProduct(services = injector.instance[InventoryBatchProductServices]),
      factoryFor(orders) is Order(services = injector.instance[OrderServices]),
      factoryFor(printOrders) is PrintOrder(services = injector.instance[PrintOrderServices]),
      factoryFor(products) is Product(services = injector.instance[ProductServices]),
      factoryFor(vbgAudioCheckTable) is VBGAudioCheck(services = injector.instance[VBGAudioCheckServices]),
      factoryFor(vbgEnrollUserTable) is VBGEnrollUser(services = injector.instance[VBGEnrollUserServices]),
      factoryFor(vbgFinishEnrollTransactionTable) is VBGFinishEnrollTransaction(services = injector.instance[VBGFinishEnrollTransactionServices]),
      factoryFor(vbgFinishVerifyTransactionTable) is VBGFinishVerifyTransaction(services = injector.instance[VBGFinishVerifyTransactionServices]),
      factoryFor(vbgStartEnrollmentTable) is VBGStartEnrollment(services = injector.instance[VBGStartEnrollmentServices]),
      factoryFor(vbgStartVerificationTable) is VBGStartVerification(services = injector.instance[VBGStartVerificationServices]),
      factoryFor(vbgVerifySampleTable) is VBGVerifySample(services = injector.instance[VBGVerifySampleServices]),
      factoryFor(xyzmoAddProfileTable) is XyzmoAddProfile(services = injector.instance[XyzmoAddProfileServices]),
      factoryFor(xyzmoAddUserTable) is XyzmoAddUser(services = injector.instance[XyzmoAddUserServices]),
      factoryFor(xyzmoDeleteUserTable) is XyzmoDeleteUser(services = injector.instance[XyzmoDeleteUserServices]),
      factoryFor(xyzmoEnrollDynamicProfileTable) is XyzmoEnrollDynamicProfile(services = injector.instance[XyzmoEnrollDynamicProfileServices]),
      factoryFor(xyzmoVerifyUserTable) is XyzmoVerifyUser(services = injector.instance[XyzmoVerifyUserServices])
    )
  }
}
