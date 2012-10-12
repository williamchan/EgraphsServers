package models.filters

import services.{Time, AppConfig}
import services.db.KeyedCaseClass
import models.HasCreatedUpdated
import java.sql.Timestamp


case class FilterValueRelationship(
  id: Long = 0L,
  filterId: Long = 0L,
  filterValueId: Long = 0L,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = FilterValueRelationship.unapply(this)
}
