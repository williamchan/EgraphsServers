package models.filters

import services.{Time, AppConfig}
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import models.{SavesCreatedUpdated, HasCreatedUpdated}
import java.sql.Timestamp
import com.google.inject.{Provider, Inject}


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
  // Public methods
  //

  def save(): CelebrityFilterValue = {
    require(celebrityId != 0, "CelebrityFilterValue: celebrity id must be specified")
    require(filterValueId != 0, "CelebrityFilterValue: filter value id must be specified")
    services.celebrityFilterValueStore.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = CelebrityFilterValue.unapply(this)
}

class CelebrityFilterValueStore @Inject() (
  schema: Schema,
  filterServices: Provider[FilterServices]
) extends SavesWithLongKey[CelebrityFilterValue]
  with SavesCreatedUpdated[Long, CelebrityFilterValue]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[FilterValue] methods
  //
  override val table = schema.celebrityFilterValues

  override def defineUpdate(theOld: CelebrityFilterValue, theNew: CelebrityFilterValue) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.filterValueId := theNew.filterValueId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,FilterValue] methods
  //
  override def withCreatedUpdated(toUpdate: CelebrityFilterValue, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
