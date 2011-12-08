package db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Table}
import models._
import java.lang.IllegalStateException
import play.Play
import java.io.{ByteArrayOutputStream, PrintWriter}

/**
 * Egraphs Database schema
 *
 * When inspecting the schema of a database table, inspect both this object and the KeyedCaseClass.
 */
object Schema extends org.squeryl.Schema {

  val voiceEnrollmentAttempts = table[VoiceEnrollmentAttempt]
  val voiceSamples = table[VoiceSample]
  val signatureEnrollmentAttempts = table[SignatureEnrollmentAttempt]
  val signatureSamples = table[SignatureSample]

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
  productToOrders.foreignKeyDeclaration.constrainReference(onDelete setNull)

  val buyingCustomerToOrders = oneToManyRelation(customers, orders)
    .via((customer, order) => customer.id === order.buyerId)
  buyingCustomerToOrders.foreignKeyDeclaration.constrainReference(onDelete setNull)

  val recipientCustomerToOrders = oneToManyRelation(customers, orders)
    .via((customer, order) => customer.id === order.recipientId)
  recipientCustomerToOrders.foreignKeyDeclaration.constrainReference(onDelete setNull)

  //
  // Egraphs
  //
  val egraphs = table[Egraph]
  on(egraphs)(egraph =>
    declare(
      columns(egraph.orderId, egraph.stateValue) are (unique)
    )
  )

  val orderToEgraphs = oneToManyRelation(orders, egraphs)
    .via((order, egraph) => order.id === egraph.orderId)
  orderToEgraphs.foreignKeyDeclaration.constrainReference(onDelete setNull)


  //
  // Public methods
  //
  /**Clears out the schema and recreates it. For God's sake don't do this in production. */
  def scrub() {
    Play.configuration.get("db.allowscrub") match {
      case "yes" =>
        if (db.Schema.isInPlace) {
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

  /** Drops the public schema of the database. This requires specific syntax for some providers. */
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
        select(celeb)
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

}
