package models.checkout

import org.joda.money.Money
import org.squeryl.KeyedEntity
import java.sql.Timestamp
import services.Time

trait LineItem[+TransactedT] {
  def _entity: LineItemEntity
  def itemType: LineItemType[TransactedT]
  def description: String
  def amount: Money

  def subItems: IndexedSeq[LineItem[_]]


  /**
   * @return flat sequence of this LineItem and its sub-LineItems, with the sub items of
   *         each item remaining in place (as opposed to being stripped, resulting in each
   *         LineItem's subItems being an empty sequence).
   */
  def flatten: IndexedSeq[LineItem[_]] = {
    val flatSubItemSeqSeq = for(subItem <- subItems) yield subItem.flatten
    IndexedSeq(this) ++ flatSubItemSeqSeq.flatten
  }

  def transact: TransactedT
}

case class LineItemEntity(
  id: Long = 0L,
  checkoutId: Long = 0L,
  itemTypeId: Long = 0L,
  _amountInCurrency: BigDecimal = BigDecimal(0),
  notes: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedEntity[Long]

// TODO(SER-499): helpers for creating line item entities
