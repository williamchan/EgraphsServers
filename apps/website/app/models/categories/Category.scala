package models.categories

import services._
import com.google.inject.{Provider, Inject}
import db.{Schema, KeyedCaseClass, SavesWithLongKey, Deletes}
import java.sql.Timestamp
import services.Time
import models._
import org.squeryl.Query
import org.squeryl.dsl.ManyToMany

case class CategoryServices @Inject() (
  celebrityCategoryValueStore: CelebrityCategoryValueStore,
  categoryStore: CategoryStore,
  categoryValueStore: CategoryValueStore,
  categoryValueRelationshipStore: CategoryValueRelationshipStore,
  celebrityStore: CelebrityStore,
  schema: Schema
)

/**
 * Class representing a category (like League, Team, Instrument for the celebrity marketplace.
 *
 * @param id
 * @param name Unique name to simplify management for Administrators
 * @param publicname Publicly facing name displayed in the marketplace to users
 * @param created
 * @param updated
 * @param services
**/

case class Category(
  id: Long = 0L,
  name: String = "",
  publicName: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CategoryServices = AppConfig.instance[CategoryServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public Methods
  //

  def save(): Category = {
    require(!name.isEmpty, "Category: name must be specified")
    require(!publicName.isEmpty, "Category: publicName must be specified")
    services.categoryStore.save(this)
  }

  def categoryValues : Query[CategoryValue] = {
    services.categoryValueStore.findByCategoryId(id)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Category.unapply(this)

}

class CategoryStore @Inject() (
  schema: Schema
) extends SavesWithLongKey[Category] with Deletes[Long, Category]
  with SavesCreatedUpdated[Category]
{
  import org.squeryl.PrimitiveTypeMode._
  
  /**
   * Returns child Categories
   */
  def categories(categoryValue: CategoryValue): Query[Category] with ManyToMany[Category, CategoryValueRelationship] = {
    schema.categoryValueRelationships.left(categoryValue)
  }

  /**
   * Returns all categories
   */
  def getCategories: Query[Category] = {
    from(schema.categories)(
      f =>
      select(f)
    )
  }
  /**
   * Find a category by name
   */
  def findByName(name: String) : Option[Category] = {
    from(schema.categories)( category =>
      where(category.name === name)
      	select(category)
      ).headOption
  }

  //
  // SavesWithLongKey[Category] methods
  //
  override val table = schema.categories



  //
  // SavesCreatedUpdated[Category] methods
  //
  override def withCreatedUpdated(toUpdate: Category, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}



