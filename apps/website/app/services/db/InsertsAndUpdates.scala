package services.db

import org.squeryl.{Table, KeyedEntity}
import services.AppConfig

/**
 * Provides any persistence-associated class that mixes it in with means to insert
 * and update an entity.
 *
 * Usage:
 * {{{
 *   case class Person(id: Long = 0L, name: String = "", created: Date = new Date(0L)) extends KeyedEntity[Long]
 *
 *   object Person extends InsertsAndUpdates[Person] {
 *     override val table: Table[Person] = MySquerylDb.people
 *
 *     def testInsert {
 *       val inserted = insert(Person())
 *       assert(inserted.id != 0)
 *
 *       val updated = update(inserted.copy(name="Joey"))
 *       assert(updated.name == "Joey")
 *     }
 *   }
 * }}}
 *
 * @tparam T the entity's type
 */
trait InsertsAndUpdates[T <: KeyedEntity[_]] extends InsertAndUpdateHooks[T] {
  /** Insert an instance into the table after processing registered pre-insert hooks */
  def insert(toInsert: T): T = {
    table.insert(performPreInsertHooks(toInsert))
  }

  /** Updates an instance in the table after processing registered pre-update hooks */
  def update(toUpdate: T): T = {
    val finalEntity = performPreUpdateHooks(toUpdate)

    table.update(finalEntity)

    finalEntity
  }

  //
  // Abstract members
  //
  /**The table that manages this entity in services.db.Schema  */
  protected def table: Table[T]
}


trait CanInsertAndUpdateThroughServices[T <: KeyedEntity[_]] { this: T =>
  def services: InsertsAndUpdates[T]
  def insert(): T = services.insert(this)
  def update(): T = services.update(this)
}


/**
 * Mixin for Model classes with separate Entities to be able to persist their entities through services as if
 * persisting themselves.
 */
trait CanInsertAndUpdateAsThroughServices[ModelT <: HasEntity[EntityT, _], EntityT <: KeyedCaseClass[_]]{
  this: ModelT =>

  def services: InsertsAndUpdatesAsEntity[ModelT, EntityT]
  def insert(): ModelT = services.insert(this)
  def update(): ModelT = services.update(this)
}


/**
 * Mixin for Model classes with separate Entities need to be serializable and able to persist their entities through
 * services as if persisting themselves.
 */
trait CanInsertAndUpdateAsThroughTransientServices[
  ModelT <: HasEntity[EntityT, _],
  EntityT <: KeyedCaseClass[_],
  ServiceT <: InsertsAndUpdatesAsEntity[ModelT, EntityT]
] extends HasTransientServices[ServiceT] { this: ModelT with Serializable =>

  def insert()(implicit manifest: Manifest[ServiceT]): ModelT = services.insert(this)
  def update()(implicit manifest: Manifest[ServiceT]): ModelT = services.update(this)
}


/**
 * Helper for making Types that take services as an argument serializable by marking the services as `@transient`.
 * Transient fields are null on deserialization, so this defaults to the injected services instance when null.
 */
trait HasTransientServices[T] { this: Serializable =>
  /** services parameter */
  def _services: T

  /** accessor, defaults to injected instance */
  def services(implicit manifest: Manifest[T]): T = if (_services == null) _services else defaultServices

  /** fallback for after deserialization */
  protected def defaultServices(implicit manifest: Manifest[T]): T = AppConfig.instance[T]
}