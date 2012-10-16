package models.filters

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{SavesWithLongKey, Schema, KeyedCaseClass}
import com.google.inject.{Inject, Provider}
import models.{HasCreatedUpdated, SavesCreatedUpdated}


case class FilterValue(
  id: Long = 0L,
  name: String = "",
  publicname: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{

  def save(): FilterValue = {
    require(!name.isEmpty, "FilterValue: name must be specified")
    require(!publicname.isEmpty, "FilterValue: publicname must be specified")
    services.filterValueStore.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = FilterValue.unapply(this)
}

class FilterValueStore @Inject() (
  schema: Schema,
  filterServices: Provider[FilterServices]
) extends SavesWithLongKey[FilterValue]
  with SavesCreatedUpdated[Long, FilterValue]
{
  import org.squeryl.PrimitiveTypeMode._

  // TODO: sbilstein
  // def findByFilterId(filterId: Long) : List[FilterValue] = ???

  //
  // SavesWithLongKey[FilterValue] methods
  //
  override val table = schema.filterValues

  override def defineUpdate(theOld: FilterValue, theNew: FilterValue) = {
    updateIs(
      theOld.publicname := theNew.publicname,
      theOld.name := theNew.name,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,FilterValue] methods
  //
  override def withCreatedUpdated(toUpdate: FilterValue, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}