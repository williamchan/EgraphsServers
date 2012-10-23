package models.filters

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{SavesWithLongKey, Schema, KeyedCaseClass}
import com.google.inject.{Inject, Provider}
import models.{Celebrity, HasCreatedUpdated, SavesCreatedUpdated}
import org.squeryl.Query
import org.squeryl.dsl.ManyToMany

/**
 * Represents specific values of a filter. For example, for the Vertical Filter,
 * some possible FilterValues can be Baseball or Soccer
 * @param id
 * @param filterId Id of parent filter (e.g. Genre)
 * @param name Unique name to aid in administration
 * @param publicname Publicly facing name to be displayed in the view
 * @param created
 * @param updated
 * @param services
 */

case class FilterValue(
  id: Long = 0L,
  filterId: Long = 0L,
  name: String = "",
  publicname: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  /**
   * Filters owned by the FilterValue
   */
  lazy val filters = services.filterStore.filters(this)
  lazy val celebrities = services.celebrityStore.celebrities(this)

  def save(): FilterValue = {
    require(!name.isEmpty, "FilterValue: name must be specified")
    require(!publicname.isEmpty, "FilterValue: publicname must be specified")
    require(filterId != 0, "FilterValue: filterId must be provided")
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

  /**
   * Return FilterValues that are tags of the specified filter
   * @param filterId
   * @return
   */
  def findByFilterId(filterId: Long) : Query[FilterValue] = {
    from(schema.filterValues)(
      (fv) =>
       where(fv.filterId === filterId)
       select(fv)
    )
  }

  def filterValues(celebrity: Celebrity): Query[FilterValue] with ManyToMany[FilterValue, CelebrityFilterValue] = {
    schema.celebrityFilterValues.left(celebrity)
  }

  //
  // SavesWithLongKey[FilterValue] methods
  //
  override val table = schema.filterValues

  override def defineUpdate(theOld: FilterValue, theNew: FilterValue) = {
    updateIs(
      theOld.publicname := theNew.publicname,
      theOld.name := theNew.name,
      theOld.filterId := theNew.filterId,
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