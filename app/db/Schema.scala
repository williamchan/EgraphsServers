package db

import org.squeryl.PrimitiveTypeMode._
import models.{Administrator, Celebrity, Customer, Account, Product}
import org.squeryl.{KeyedEntity, Table}

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
    via((celebrity, product)=> celebrity.id === product.celebrityId)


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

    relation.foreignKeyDeclaration.constrainReference(onDelete setNull)

    relation
  }
}
