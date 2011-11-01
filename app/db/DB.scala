package db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import models.{EgraphsCustomer, Credential}

/**
 * Egraphs Database schema
 */
object DB extends Schema {
  //
  // Credentials
  //
  val credentials = table[Credential]
  on(credentials)(credential => declare(credential.email is (unique)))

  
  //
  // Customers
  //
  val customers = table[EgraphsCustomer]
  on(customers)(customer => declare(
    customer.credentialId is(unique)
  ))

  val customerToCredential = oneToManyRelation(credentials, customers)
    .via((credential, customer) => customer.credentialId === credential.id)

  customerToCredential.foreignKeyDeclaration.constrainReference(onDelete cascade)

  /*val customerWithCredential = join(customers, credentials)((cust, cred) =>
    select(cust, cred) on (cred.id === cust.credentialId)
  )

  val getSumpin = from(customers)(customer =>
    where(customer.id === 2)
      select(EgraphsCustomer.unapply(customer))
  )*/

  //
  // Public methods
  //
  /** Clears out the schema and recreates it. For God's sake don't do this in production. */
  def scrub {
    inTransaction {
      drop
      create
    }
  }
}