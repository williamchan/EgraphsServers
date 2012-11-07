package models.categories

import services.{Time, AppConfig}
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import models.{SavesCreatedUpdated, HasCreatedUpdated}
import java.sql.Timestamp
import com.google.inject.{Provider, Inject}

/**
 * Mapping between celebrities and their tagged categoryvalues
 *
 * @param id
 * @param celebrityId
 * @param categoryValueId
 * @param services
 */

case class CelebrityCategoryValue (
  id: Long = 0L,
  celebrityId: Long = 0l,
  categoryValueId: Long = 0L,
  services: CategoryServices = AppConfig.instance[CategoryServices]
) extends KeyedCaseClass[Long]
{
  //
  // Public methods
  //

  def save(): CelebrityCategoryValue = {
    services.celebrityCategoryValueStore.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = CelebrityCategoryValue.unapply(this)
}

class CelebrityCategoryValueStore @Inject() (
  schema: Schema,
  categoryServices: Provider[CategoryServices]
) extends SavesWithLongKey[CelebrityCategoryValue]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[CategoryValue] methods
  //
  override val table = schema.celebrityCategoryValues

  override def defineUpdate(theOld: CelebrityCategoryValue, theNew: CelebrityCategoryValue) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.categoryValueId := theNew.categoryValueId
    )
  }
}
