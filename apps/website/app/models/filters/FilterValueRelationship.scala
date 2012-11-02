package models.filters

import services.{Time, AppConfig}
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import models.{SavesCreatedUpdated, HasCreatedUpdated}
import java.sql.Timestamp
import com.google.inject.{Provider, Inject}
import org.squeryl.Query

/**
 * Represents relationships between a filterValue and a filter
 *
 * Mapping is FilterValue -> Filter.
 * This exposes the model of a specific type of product having filters. For example,
 * if there is a top level Filter called Vertical, it may have Values like Baseball.
 * The Baseball FilterValue then owns baseball-specific filters and this relationship is exposed
 * through this table.
 *
 * @param id
 * @param filterId Id of filter (e.g. Team)
 * @param filterValueId id of FilterValue (e.g. Baseball)
 * @param services
 */
case class FilterValueRelationship(
  id: Long = 0L,
  filterId: Long = 0L,
  filterValueId: Long = 0L,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long]
{
  //
  // Public methods
  //

  def save(): FilterValueRelationship = {
    services.filterValueRelationshipStore.save(this)
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
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[FilterValueRelationship] methods
  //
  override val table = schema.filterValueRelationships

  override def defineUpdate(theOld: FilterValueRelationship, theNew: FilterValueRelationship) = {
    updateIs(
      theOld.filterId := theNew.filterId,
      theOld.filterValueId := theNew.filterValueId
    )
  }
}
