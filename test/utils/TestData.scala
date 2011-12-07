package utils

import libs.Time
import models.{Account, Customer}

/**
 * Renders saved copies of domain objects that satisfy all relational integrity
 * constraints.
 */
object TestData {
  def newSavedCustomer(): Customer = {
    val acct = Account(email=Time.now.toString).save()
    val cust = Customer().save()

    acct.copy(customerId=Some(cust.id)).save()

    cust
  }
}