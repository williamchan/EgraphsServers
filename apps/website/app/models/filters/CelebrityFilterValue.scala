package models.filters

import services.{Time, AppConfig}
import services.db.KeyedCaseClass
import models.HasCreatedUpdated
import java.sql.Timestamp


case class CelebrityFilterValue (
  id: Long = 0L,
  celebrityId: Long = 0l,
  filterValueId: Long = 0L,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = CelebrityFilterValue.unapply(this)
}
