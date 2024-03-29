package models.checkout

import java.sql.Timestamp
import org.joda.money.{CurrencyUnit, Money}
import models.HasCreatedUpdated
import services.db.KeyedCaseClass
import services.Time


case class LineItemEntity(
  id: Long = 0,
  _checkoutId: Long = 0,
  _itemTypeId: Long = 0,
  _amountInCurrency: BigDecimal = BigDecimal(0),
  notes: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = LineItemEntity.unapply(this)
  def amount = Money.of(CurrencyUnit.USD, _amountInCurrency.bigDecimal)
  def withAmount(newAmount: Money) = this.copy(_amountInCurrency = newAmount.getAmount)
}

object LineItemEntity {
  def apply(amount: Money, notes: String): LineItemEntity = apply(amount, notes, 0)

  def apply(amount: Money, notes: String, typeId: Long): LineItemEntity = {
    new LineItemEntity(
      _itemTypeId = typeId,
      _amountInCurrency = amount.getAmount,
      notes = notes
    )
  }
}
