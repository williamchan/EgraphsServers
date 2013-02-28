package models.categories

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{SavesWithLongKey, Schema, KeyedCaseClass, Deletes}
import com.google.inject.Inject
import models.{Masthead, Celebrity, HasCreatedUpdated, SavesCreatedUpdated}
import org.squeryl.Query
import org.squeryl.dsl.ManyToMany

/**
 * Represents specific values of a category. For example, for the Vertical Category,
 * some possible CategoryValues can be Baseball or Soccer
 * @param id
 * @param categoryId Id of parent category (e.g. Genre)
 * @param name Unique name to aid in administration
 * @param publicName Publicly facing name to be displayed in the view
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
  lazy val mastheads = services.mastheadStore.mastheads(this)

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
  schema: Schema
) extends SavesWithLongKey[CategoryValue]
  with SavesCreatedUpdated[CategoryValue]
  with Deletes[Long, CategoryValue]
{
  import org.squeryl.PrimitiveTypeMode._

  /**
   * Return all CategoryValues.
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

  def categoryValues(masthead: Masthead): Query[CategoryValue] with ManyToMany[CategoryValue, MastheadCategoryValue] = {
    schema.mastheadCategoryValues.left(masthead)
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
   * Updates categories owned by a given CategoryValue.  
   */
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

  /**
   * Update a category value's associated celebrities
   */
  def updateCelebrities(categoryValue: CategoryValue, celebrityIds: Iterable[Long]) {
    //remove old records
    categoryValue.celebrities.dissociateAll

    // Add records for the new values
    val newCelebrityCategoryValues = for (celebrityId <- celebrityIds) yield {
      CelebrityCategoryValue(celebrityId = celebrityId, categoryValueId = categoryValue.id)
    }

    schema.celebrityCategoryValues.insert(
      newCelebrityCategoryValues
    )
  }

  /**
   * Update a masthead
   */

  def updateMastheads(categoryValue: CategoryValue, mastheadIds: Iterable[Long]) {
    categoryValue.mastheads.dissociateAll

    val newMastheadCategoryValues = for (mastheadId <-  mastheadIds)  yield {
      MastheadCategoryValue(mastheadId = mastheadId, categoryValueId = categoryValue.id)
    }

    schema.mastheadCategoryValues.insert(
      newMastheadCategoryValues
    )
  }

  //
  // SavesWithLongKey[CategoryValue] methods
  //
  override val table = schema.categoryValues

  //
  // SavesCreatedUpdated[CategoryValue] methods
  //
  override def withCreatedUpdated(toUpdate: CategoryValue, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}