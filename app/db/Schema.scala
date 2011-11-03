package db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Table
import models.{Administrator, Celebrity, Customer, Account}

/**
 * Egraphs Database schema
 */
object Schema extends org.squeryl.Schema {
  //
  // Accounts
  //
  val accounts = table[Account]
  on(accounts)(account => declare(account.email is (unique)))

  /**
   * Establishes and returns a one-to-one relationship against Account.
   *
   * Does this by setting a unique index on the table's accountId field
   * then enforces a foreign key constraint against accounts.id.
   */
  def oneToOneRelationOnAccount[T <: { def accountId: Long }]
      (table: Table[T]): OneToManyRelationImpl[Account, T] =
  {
    // Unique index on account ID
    on(table)(row => declare(row.accountId is(unique)))

    // Foreign key against account
    val relation = oneToManyRelation(accounts, table)
      .via((account, tableRow) => tableRow.accountId === account.id)

    // Deleting the account cascades to this table
    relation.foreignKeyDeclaration.constrainReference(onDelete cascade)

    relation
  }

  //
  // Customers
  //
  val customers = table[Customer]
  val customersToAccounts = oneToOneRelationOnAccount(customers)
  
  //
  // Celebrities
  //
  val celebrities = table[Celebrity]
  val celebritiesToAccounts = oneToOneRelationOnAccount(celebrities)

  //
  // Administrators
  //
  val administrators = table[Administrator]
  val administratorsToAccounts = oneToOneRelationOnAccount(administrators)


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
}
