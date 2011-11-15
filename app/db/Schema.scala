package db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Table}
import models._

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
      columns(egraph.orderId, egraph.stateValue) are (indexed)
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
    inTransaction {
      drop
      create
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
