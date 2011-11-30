package db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Table}
import models._
import java.lang.IllegalStateException
import play.Play
import java.io.{ByteArrayOutputStream, PrintWriter}

/**
 * Egraphs Database schema
 */
object Schema extends org.squeryl.Schema {
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
  // Accounts
  //
  val accounts = table[Account]
  on(accounts)(account => declare(account.email is (unique)))

  val accountToCustomer = oneAccountPerRowOn(customers, (account) => account.customerId)
  val accountToAdministrator = oneAccountPerRowOn(administrators, (acct) => acct.administratorId)
  val accountToCelebrity = oneAccountPerRowOn(celebrities, (account) => account.celebrityId)

  //
  // Products
  //
  val products = table[Product]
  on(products)(product=>
    declare(
      columns(product.celebrityId, product.urlSlug) are (unique)
    )
  )
  val celebrityToProduct = oneToManyRelation(celebrities, products).
    via((celebrity, product) => celebrity.id === product.celebrityId)

  //
  // Orders
  //
  val orders = table[Order]("Orders")

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
  /** Clears out the schema and recreates it. For God's sake don't do this in production. */
  def scrub() {
    Play.configuration.get("db.allowscrub") match {
      case "yes" =>
        drop
        create

      case _ =>
        throw new IllegalStateException(
          """I'm just not going to scrub the DB unless "db.allowscrub" is
          set to "yes" in application.conf. Sorry if you have a problem with that."""
        )
    }
  }

  /** Returns the SQL commands that define this schema as a String. */
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
   */
  def isInPlace: Boolean = {
    // Basically just try to perform a query and if it throws up it doesn't exist.
    try {
      from(celebrities)(celeb =>
        select(celeb)
      ).headOption

      true
    } catch {
      case e: RuntimeException if e.getMessage.toLowerCase.contains("celebrity") =>
        false

      case otherErrors =>
        throw otherErrors
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
  def oneAccountPerRowOn[T <: KeyedEntity[Long]]
    (table: Table[T], foreignKey: Account=>Option[Long]): OneToManyRelationImpl[T, Account] =
  {
    val relation = oneToManyRelation(table, accounts)
      .via((row, account) => row.id === foreignKey(account))

    on(accounts)(account => declare(foreignKey(account) is(unique)))
    relation.foreignKeyDeclaration.constrainReference(onDelete setNull)

    relation
  }
}
