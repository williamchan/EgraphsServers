package models.checkout

import org.joda.money.Money

trait Checkout {
  def lineItems: Seq[LineItem[_]]

  def subtotal: Money
  def total: Money
}
