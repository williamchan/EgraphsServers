package models.categories

import services.AppConfig
import services.db.{Deletes, Schema, SavesWithLongKey, KeyedCaseClass}
import com.google.inject.{Provider, Inject}
import org.squeryl.Query

/**
 * Mapping between mastheads and their tagged categoryvalues
 *
 * @param id
 * @param mastheadId
 * @param categoryValueId
 * @param services
 */
case class MastheadCategoryValue (
                                    id: Long = 0L,
                                    mastheadId: Long = 0L,
                                    categoryValueId: Long = 0L,
                                    services: CategoryServices = AppConfig.instance[CategoryServices]
                                    ) extends KeyedCaseClass[Long]
{
  //
  // Public methods
  //

  def save(): MastheadCategoryValue = {
    services.mastheadCategoryValueStore.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = MastheadCategoryValue.unapply(this)
}

class MastheadCategoryValueStore @Inject() (
                                              schema: Schema,
                                              categoryServices: Provider[CategoryServices]
                                              ) extends SavesWithLongKey[MastheadCategoryValue] with Deletes[Long, MastheadCategoryValue]
{
  import org.squeryl.PrimitiveTypeMode._

  def findByCategoryValueId(categoryValueId: Long): Query[MastheadCategoryValue] = {
    from(schema.mastheadCategoryValues)( mcv =>
      where(mcv.categoryValueId === categoryValueId)
        select(mcv)
    )
  }

  //
  // SavesWithLongKey[CategoryValue] methods
  //
  override val table = schema.mastheadCategoryValues


}
