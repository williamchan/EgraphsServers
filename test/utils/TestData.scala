package utils

import libs.Time
import models.{Account, Customer}

object TestData {
  def newSavedCustomer(): Customer = {
    val acct = Account(email=Time.now.toString).save()
    val cust = Customer().save()

    acct.copy(customerId=Some(cust.id)).save()

    cust
  }
}