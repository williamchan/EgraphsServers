package models.categories

import services.{Time, AppConfig}
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import models.{SavesCreatedUpdated, HasCreatedUpdated}
import java.sql.Timestamp
import com.google.inject.{Provider, Inject}
import org.squeryl.Query

/**
 * Represents relationships between a categoryValue and a category
 *
 * Mapping is CategoryValue -> Category.
 * This exposes the model of a specific type of product having categories. For example,
 * if there is a top level Category called Vertical, it may have Values like Baseball.
 * The Baseball CategoryValue then owns baseball-specific categories and this relationship is exposed
 * through this table.
 *
 * @param id
 * @param categoryId Id of category (e.g. Team)
 * @param categoryValueId id of CategoryValue (e.g. Baseball)
 * @param services
 */
case class CategoryValueRelationship(
  id: Long = 0L,
  categoryId: Long = 0L,
  categoryValueId: Long = 0L,
  services: CategoryServices = AppConfig.instance[CategoryServices]
) extends KeyedCaseClass[Long]
{
  //
  // Public methods
  //

  def save(): CategoryValueRelationship = {
    services.categoryValueRelationshipStore.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = CategoryValueRelationship.unapply(this)
}

class CategoryValueRelationshipStore @Inject() (
  schema: Schema,
  categoryServices: Provider[CategoryServices]
) extends SavesWithLongKey[CategoryValueRelationship]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[CategoryValueRelationship] methods
  //
  override val table = schema.categoryValueRelationships

  override def defineUpdate(theOld: CategoryValueRelationship, theNew: CategoryValueRelationship) = {
    updateIs(
      theOld.categoryId := theNew.categoryId,
      theOld.categoryValueId := theNew.categoryValueId
    )
  }
}
