package models

import java.sql.Timestamp
import libs.Time
import org.squeryl.PrimitiveTypeMode._
import db.{KeyedCaseClass, Schema, Saves}

/**
 * Persistent entity representing customers who buy products from our service.
 */
case class Customer(
  id: Long = 0L,
  name: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public methods
  //
  def save(): Customer = {
    Customer.save(this)
  }

  /**
   * Orders an Egraph product on behalf of a recipient. The results must be persisted for the transaction
   * to actually accomplish anything.
   *
   * @param product the Product to buy
   * @param recipient the Customer receiving the eGraph. Defaults to the purchasing Customer.
   *
   * @return a tuple of the Order for the Product purchased by this Customer for the recipient Customer
   *   and the transaction that took place.
   */
  def buy(product: Product, recipient: Customer = this): Order = {
    Order(
      buyerId=id,
      recipientId=recipient.id,
      productId=product.id,
      amountPaidInCurrency=BigDecimal(product.price.getAmount)
    )
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Customer.unapply(this)
  }
}

object Customer extends Saves[Customer] with SavesCreatedUpdated[Customer] {
  //
  // Public members
  //

  /**
   * Either retrieves an existing Customer keyed by the provided e-mail address
   * or creates one, saves it to the database, then returns it.
   */
  def findOrCreateByEmail(email: String): Customer = {
    // TODO: Optimize this using a single outer-join query to get Customer
    // and Account all at once

    // Get the Account and Customer face if both exist.
    val accountOption = Account.findByEmail(email)
    val customerOption = accountOption.flatMap { account =>
      account.customerId.flatMap { customerId =>
        Customer.findById(customerId)
      }
    }

    // Handle various cases of either the account or its Customer face not existing
    (accountOption, customerOption) match {
      // Both Account and its Customer face already existed
      case (Some(account), Some(customer)) =>
        customer

      // Have an Account but no Customer face. Make a Customer face and save both.
      case (Some(account), None) =>
        val customer = Customer().save()

        account.copy(customerId=Some(customer.id)).save()

        customer

      // Have neither Account nor (by definition) Customer face. Make both.
      case (None, _) =>
        val customer = Customer().save()
        Account(email=email, customerId=Some(customer.id)).save()

        customer
    }
  }

  //
  // Saves[Customer] methods
  //
  override val table = Schema.customers

  override def defineUpdate(theOld: Customer, theNew: Customer) = {
    updateIs(
      theOld.name  := theNew.name,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Customer] methods
  //
  override def withCreatedUpdated(toUpdate: Customer, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}