package models.frontend.storefront

import org.joda.money.Money

case class FinalizePriceViewModel (
  base: Money,
  physicalGood: Option[Money],
  tax: Option[Money],
  total: Money
)
