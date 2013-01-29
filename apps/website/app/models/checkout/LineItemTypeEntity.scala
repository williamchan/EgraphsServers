package models.checkout

import java.sql.Timestamp
import models.HasCreatedUpdated
import models.enums.{CheckoutCodeType, LineItemNature}
import services.db.KeyedCaseClass
import services.Time

/**
 * A row in the LineItemType table
 */
case class LineItemTypeEntity private (
  id: Long,
  _desc: String,
  _nature: String,
  _codeType: String,
  created: Timestamp,
  updated: Timestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = LineItemTypeEntity.unapply(this)

  def nature = LineItemNature(_nature).get
  def codeType = CheckoutCodeType(_codeType).get
}

object LineItemTypeEntity {
  def apply(
    desc: String = "",
    nature: LineItemNature,
    codeType: CheckoutCodeType,
    id: Long = 0,
    created: Timestamp = Time.defaultTimestamp,
    updated: Timestamp = Time.defaultTimestamp
  ) = new LineItemTypeEntity(id, desc, nature.name, codeType.name, created, updated)
}
