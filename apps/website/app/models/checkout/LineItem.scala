package models.checkout

import org.joda.money.Money
import org.squeryl.KeyedEntity
import java.sql.Timestamp

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
  created: Timestamp,
  updated: Timestamp
) extends KeyedEntity[Long]
