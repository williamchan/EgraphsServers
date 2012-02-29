package services.db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Table}
import models._
import models.vbg._
import models.xyzmo._
import java.lang.IllegalStateException
import play.Play
import java.io.{ByteArrayOutputStream, PrintWriter}
import com.google.inject.{Inject, Injector}

/**
 * Egraphs Database schema
 *
 * When inspecting the schema of a database table, inspect both this object and the KeyedCaseClass.
 */
class Schema @Inject()(injector: Injector) extends org.squeryl.Schema {

  import uk.me.lings.scalaguice.InjectorExtensions._

  //
  // Customers
  //
  val customers = table[Customer]

  //
  // Celebrities
  //
  val celebrities = table[Celebrity]
  on(celebrities)(celebrity =>
    declare(
      celebrity.urlSlug is (unique),
      celebrity.description is (dbType("varchar(255)"))
    )
  )

  //
  // Administrators
  //
  val administrators = table[Administrator]

  //
  // Cash transactions
  //
  val cashTransactions = table[CashTransaction]
  on(cashTransactions)(cashTransaction =>
    declare(cashTransaction.amountInCurrency is monetaryDbType)
  )

  //
  // Accounts
  //
  val accounts = table[Account]
  on(accounts)(account => declare(account.email is (unique)))

  val accountToCustomer = oneAccountPerRowOn(customers, (account) => account.customerId)
  val accountToAdministrator = oneAccountPerRowOn(administrators, (acct) => acct.administratorId)
  val accountToCelebrity = oneAccountPerRowOn(celebrities, (account) => account.celebrityId)
  val accountToTransaction = oneToManyRelation(accounts, cashTransactions).via(
    (account, cashTransaction) => account.id === cashTransaction.accountId)

  //
  // Products
  //
  val products = table[Product]
  on(products)(product =>
    declare(
      product.priceInCurrency is monetaryDbType,
      columns(product.celebrityId, product.urlSlug) are (unique)
    )
  )
  val celebrityToProduct = oneToManyRelation(celebrities, products).
    via((celebrity, product) => celebrity.id === product.celebrityId)

  //
  // Orders
  //
  val orders = table[Order]("Orders")
  on(orders)(order =>
    declare(order.amountPaidInCurrency is monetaryDbType)
  )

  val productToOrders = oneToManyRelation(products, orders)
    .via((product, order) => product.id === order.productId)
  productToOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val buyingCustomerToOrders = oneToManyRelation(customers, orders)
    .via((customer, order) => customer.id === order.buyerId)
  buyingCustomerToOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val recipientCustomerToOrders = oneToManyRelation(customers, orders)
    .via((customer, order) => customer.id === order.recipientId)
  recipientCustomerToOrders.foreignKeyDeclaration.constrainReference(onDelete cascade)

  //
  // Egraphs
  //
  val egraphs = table[Egraph]
  on(egraphs)(egraph =>
    declare(
      columns(egraph.orderId, egraph.stateValue) are (indexed)
    )
  )

  val orderToEgraphs = oneToManyRelation(orders, egraphs)
    .via((order, egraph) => order.id === egraph.orderId)
  orderToEgraphs.foreignKeyDeclaration.constrainReference(onDelete cascade)


  //
  // media
  //
  val signatureSamples = table[SignatureSample]
  val voiceSamples = table[VoiceSample]


  //
  // biometrics
  //
  val enrollmentBatches = table[EnrollmentBatch]
  val enrollmentSamples = table[EnrollmentSample]

  val vbgAudioCheckTable = table[VBGAudioCheck]
  val vbgEnrollUserTable = table[VBGEnrollUser]
  val vbgFinishEnrollTransactionTable = table[VBGFinishEnrollTransaction]
  val vbgFinishVerifyTransactionTable = table[VBGFinishVerifyTransaction]
  val vbgStartEnrollmentTable = table[VBGStartEnrollment]
  val vbgStartVerificationTable = table[VBGStartVerification]
  val vbgVerifySampleTable = table[VBGVerifySample]

  val xyzmoAddUserTable = table[XyzmoAddUser]
  val xyzmoDeleteUserTable = table[XyzmoDeleteUser]
  val xyzmoAddProfileTable = table[XyzmoAddProfile]
  val xyzmoEnrollDynamicProfileTable = table[XyzmoEnrollDynamicProfile]
  val xyzmoVerifyUserTable = table[XyzmoVerifyUser]


  //
  // Public methods
  //
  /**Clears out the schema and recreates it. For God's sake don't do this in production. */
  def scrub() {
    Play.configuration.get("db.allowscrub") match {
      case "yes" =>
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
        val conn = play.db.DB.getConnection
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
    val conn = play.db.DB.getConnection
    val savepoint = conn.setSavepoint()

    try {
      from(celebrities)(celeb =>
        select(celeb.id)
      ).headOption

      true
    }
    catch {
      case e: RuntimeException if e.getMessage.toLowerCase.contains("celebrity") =>
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
  private def oneAccountPerRowOn[T <: KeyedEntity[Long]]
  (table: Table[T], foreignKey: Account => Option[Long]): OneToManyRelationImpl[T, Account] = {
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
      factoryFor(cashTransactions) is CashTransaction(services = injector.instance[CashTransactionServices]),
      factoryFor(celebrities) is Celebrity(services = injector.instance[CelebrityServices]),
      factoryFor(customers) is Customer(services = injector.instance[CustomerServices]),
      factoryFor(administrators) is Administrator(services=injector.instance[AdministratorServices]),
      factoryFor(egraphs) is Egraph(services = injector.instance[EgraphServices]),
      factoryFor(enrollmentBatches) is EnrollmentBatch(services = injector.instance[EnrollmentBatchServices]),
      factoryFor(enrollmentSamples) is EnrollmentSample(services = injector.instance[EnrollmentSampleServices]),
      factoryFor(orders) is Order(services = injector.instance[OrderServices]),
      factoryFor(products) is Product(services = injector.instance[ProductServices]),
      factoryFor(signatureSamples) is SignatureSample(services = injector.instance[SignatureSampleServices]),
      factoryFor(vbgAudioCheckTable) is VBGAudioCheck(services = injector.instance[VBGAudioCheckServices]),
      factoryFor(vbgEnrollUserTable) is VBGEnrollUser(services = injector.instance[VBGEnrollUserServices]),
      factoryFor(vbgFinishEnrollTransactionTable) is VBGFinishEnrollTransaction(services = injector.instance[VBGFinishEnrollTransactionServices]),
      factoryFor(vbgFinishVerifyTransactionTable) is VBGFinishVerifyTransaction(services = injector.instance[VBGFinishVerifyTransactionServices]),
      factoryFor(vbgStartEnrollmentTable) is VBGStartEnrollment(services = injector.instance[VBGStartEnrollmentServices]),
      factoryFor(vbgStartVerificationTable) is VBGStartVerification(services = injector.instance[VBGStartVerificationServices]),
      factoryFor(vbgVerifySampleTable) is VBGVerifySample(services = injector.instance[VBGVerifySampleServices]),
      factoryFor(voiceSamples) is VoiceSample(services = injector.instance[VoiceSampleServices]),
      factoryFor(xyzmoAddUserTable) is XyzmoAddUser(services = injector.instance[XyzmoAddUserServices]),
      factoryFor(xyzmoDeleteUserTable) is XyzmoDeleteUser(services = injector.instance[XyzmoDeleteUserServices]),
      factoryFor(xyzmoAddProfileTable) is XyzmoAddProfile(services = injector.instance[XyzmoAddProfileServices]),
      factoryFor(xyzmoEnrollDynamicProfileTable) is XyzmoEnrollDynamicProfile(services = injector.instance[XyzmoEnrollDynamicProfileServices]),
      factoryFor(xyzmoVerifyUserTable) is XyzmoVerifyUser(services = injector.instance[XyzmoVerifyUserServices])
    )
  }
}
