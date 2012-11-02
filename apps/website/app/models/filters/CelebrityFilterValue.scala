package models.filters

import services.{Time, AppConfig}
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import models.{SavesCreatedUpdated, HasCreatedUpdated}
import java.sql.Timestamp
import com.google.inject.{Provider, Inject}

/**
 * Mapping between celebrities and their tagged filtervalues
 *
 * @param id
 * @param celebrityId
 * @param filterValueId
 * @param services
 */

case class CelebrityFilterValue (
  id: Long = 0L,
  celebrityId: Long = 0l,
  filterValueId: Long = 0L,
  services: FilterServices = AppConfig.instance[FilterServices]
) extends KeyedCaseClass[Long]
{
  //
  // Public methods
  //

  def save(): CelebrityFilterValue = {
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
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[FilterValue] methods
  //
  override val table = schema.celebrityFilterValues

  override def defineUpdate(theOld: CelebrityFilterValue, theNew: CelebrityFilterValue) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.filterValueId := theNew.filterValueId
    )
  }
}
