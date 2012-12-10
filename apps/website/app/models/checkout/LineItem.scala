package models.checkout

import org.joda.money.Money
import org.squeryl.KeyedEntity
import java.sql.Timestamp
import services.Time

trait LineItem[+TransactedT] {
  def itemType: LineItemType[TransactedT]
  def description: String
  def amount: Money

  def subItems: IndexedSeq[LineItem[_]]

  def flatten: IndexedSeq[LineItem[_]]

  def transact: TransactedT
}

case class LineItemEntity(
  id: Long = 0L,
  checkoutId: Long = 0L,
  itemTypeId: Long = 0L,
  _amountInCurrency: BigDecimal = BigDecimal(0),
  notes: String,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedEntity[Long]
