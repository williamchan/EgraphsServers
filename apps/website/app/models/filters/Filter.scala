package models.filters

import services._
import com.google.inject.{Provider, Inject}
import db.{Schema, KeyedCaseClass, SavesWithLongKey}
import java.sql.Timestamp
import services.Time
import models.{SavesCreatedUpdated, HasCreatedUpdated, CelebrityStore}


case class FilterServices @Inject() (
  celebrityFilterValueStore: CelebrityFilterValueStore,
  filterStore: FilterStore,
  filterValueStore: FilterValueStore,
  filterValueRelationshipStore: FilterValueRelationshipStore,
  celebrityStore: CelebrityStore,
  schema: Schema
)

/**
 * Class representing a filter (like League, Team, Instrument for the celebrity marketplace.
 *
 *
 * @param id
 * @param name Unique name to simplify management for Admininstrators
 * @param publicname Publicly facing name displayed in the marketplace to users
 * @param created
 * @param updated
 * @param services
**/

case class Filter(
  id: Long = 0L,
  name: String = "",
  publicname: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public Methods
  //

  def save(): Filter = {
    require(!name.isEmpty, "Filter: name must be specified")
    require(!publicname.isEmpty, "Filter: publicname must be specified")
    services.filterStore.save(this)
  }

  def include(filterValue: FilterValue) : Filter = {
    this
  }

//  def filterValues : List[FilterValue] = {
//    services.filterValueStore.findByFilterId(id)
//  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Filter.unapply(this)

}

class FilterStore @Inject() (
  schema: Schema,
  filterServices: Provider[FilterServices]
) extends SavesWithLongKey[Filter]
  with SavesCreatedUpdated[Long,Filter]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[Filter] methods
  //
  override val table = schema.filters

  override def defineUpdate(theOld: Filter, theNew: Filter) = {
    updateIs(
      theOld.publicname := theNew.publicname,
      theOld.name := theNew.name,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,Filter] methods
  //
  override def withCreatedUpdated(toUpdate: Filter, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}



