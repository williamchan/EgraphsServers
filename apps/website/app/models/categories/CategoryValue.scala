package models.categories

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{SavesWithLongKey, Schema, KeyedCaseClass}
import com.google.inject.{Inject, Provider}
import models.{Celebrity, HasCreatedUpdated, SavesCreatedUpdated}
import org.squeryl.Query
import org.squeryl.dsl.ManyToMany

/**
 * Represents specific values of a category. For example, for the Vertical Category,
 * some possible CategoryValues can be Baseball or Soccer
 * @param id
 * @param categoryId Id of parent category (e.g. Genre)
 * @param name Unique name to aid in administration
 * @param publicname Publicly facing name to be displayed in the view
 * @param created
 * @param updated
 * @param services
 */

case class CategoryValue(
  id: Long = 0L,
  categoryId: Long = 0L,
  name: String = "",
  publicName: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CategoryServices = AppConfig.instance[CategoryServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  /**
   * Categories owned by the CategoryValue
   */
  lazy val categories = services.categoryStore.categories(this)
  lazy val celebrities = services.celebrityStore.celebrities(this)

  def save(): CategoryValue = {
    require(!name.isEmpty, "CategoryValue: name must be specified")
    require(!publicName.isEmpty, "CategoryValue: publicName must be specified")
    services.categoryValueStore.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = CategoryValue.unapply(this)
}

class CategoryValueStore @Inject() (
  schema: Schema,
  categoryServices: Provider[CategoryServices]
) extends SavesWithLongKey[CategoryValue]
  with SavesCreatedUpdated[Long, CategoryValue]
{
  import org.squeryl.PrimitiveTypeMode._

  /**
   * Return all CategoryValues.
   * @param categoryId
   * @return
   */
  def all() : Query[CategoryValue] = {
    from(schema.categoryValues)(
      (categoryValue) =>
       where(1 === 1)
       select(categoryValue)
    )
  }

  /**
   * Return CategoryValues that are tags of the specified category.
   * @param categoryId
   * @return
   */
  def findByCategoryId(categoryId: Long) : Query[CategoryValue] = {
    from(schema.categoryValues)(
      (categoryValue) =>
       where(categoryValue.categoryId === categoryId)
       select(categoryValue)
    )
  }

  /**
   * Return tuples of CategoryValues and their Categories.
   */
  def findCategoryValueCategoryViewModel : Query[(CategoryValue, Category)]  = {
    join(schema.categoryValues, schema.categories)((categoryValue, category) =>
      select(categoryValue, category)
      on(categoryValue.categoryId === category.id)
    )
  }
  
  /**
   * Find a CategoryValue by name.
   */
  def findByName(name: String) : Option[CategoryValue] = {
    from(schema.categoryValues)( categoryValue =>
      where(categoryValue.name === name)
      	select(categoryValue)
      ).headOption
  }

  def categoryValues(celebrity: Celebrity): Query[CategoryValue] with ManyToMany[CategoryValue, CelebrityCategoryValue] = {
    schema.celebrityCategoryValues.left(celebrity)
  }
  
  /**
   * Find CategoryValue pairs of a given celebrity. 
   */
  def categoryValueCategoryPairs(celebrity: Celebrity): Query[(CategoryValue, Category)] = {
    from(schema.categoryValues, schema.categories, schema.celebrityCategoryValues)((fv, f, cfv) =>
      where(cfv.celebrityId === celebrity.id and fv.id === cfv.categoryValueId and f.id === fv.categoryId)
      select((fv, f))
    )
  }

  /**
   *  Updates categories owned by a given CategoryValue.  
   **/
  def updateCategories(categoryValue: CategoryValue, categoryIds: Iterable[Long]) = {
    //remove old records
    categoryValue.categories.dissociateAll

    // Add records for the new values
    val newCategoryValueRelationships  = for (categoryId <- categoryIds) yield 
    { 
      CategoryValueRelationship(categoryId = categoryId , categoryValueId = categoryValue.id)
    }

    schema.categoryValueRelationships.insert(
       newCategoryValueRelationships
    )
  }

  //
  // SavesWithLongKey[CategoryValue] methods
  //
  override val table = schema.categoryValues

  override def defineUpdate(theOld: CategoryValue, theNew: CategoryValue) = {
    updateIs(
      theOld.publicName := theNew.publicName,
      theOld.name := theNew.name,
      theOld.categoryId := theNew.categoryId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,CategoryValue] methods
  //
  override def withCreatedUpdated(toUpdate: CategoryValue, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}