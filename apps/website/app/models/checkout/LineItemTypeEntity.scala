package models.checkout

import java.sql.Timestamp
import services.db.KeyedCaseClass
import models.HasCreatedUpdated
import org.squeryl.annotations.Transient
import models.enums.{CodeType, LineItemNature}
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
  @Transient override lazy val unapplied = LineItemTypeEntity.unapply(this)

  def nature = LineItemNature(_nature).get
  def codeType = CodeType(_codeType).get
}

object LineItemTypeEntity {
  def apply(
    desc: String = "",
    nature: LineItemNature,
    codeType: CodeType,
    id: Long = 0,
    created: Timestamp = Time.defaultTimestamp,
    updated: Timestamp = Time.defaultTimestamp
  ) = new LineItemTypeEntity(id, desc, nature.name, codeType.name, created, updated)
}
