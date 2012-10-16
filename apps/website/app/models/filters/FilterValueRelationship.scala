package models.filters

import services.{Time, AppConfig}
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import models.{SavesCreatedUpdated, HasCreatedUpdated}
import java.sql.Timestamp
import com.google.inject.{Provider, Inject}


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
  // Public methods
  //

  def save(): FilterValueRelationship = {
    require(filterId != 0, "FilterValueRelationship: filter id must be specified")
    require(filterValueId != 0, "FilterValueRelationship: filter value id must be specified")
    services.filterValueRelationshipStore.save(this)
//    this
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = FilterValueRelationship.unapply(this)
}

class FilterValueRelationshipStore @Inject() (
  schema: Schema,
  filterServices: Provider[FilterServices]
) extends SavesWithLongKey[FilterValueRelationship]
  with SavesCreatedUpdated[Long, FilterValueRelationship]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[FilterValueRelationship] methods
  //
  override val table = schema.filterValueRelationships

  override def defineUpdate(theOld: FilterValueRelationship, theNew: FilterValueRelationship) = {
    updateIs(
      theOld.filterId := theNew.filterId,
      theOld.filterValueId := theNew.filterValueId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,FilterValueRelationship] methods
  //
  override def withCreatedUpdated(toUpdate: FilterValueRelationship, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
