package services.db

import org.squeryl.{Table, KeyedEntity}

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

@deprecated(
  "SER-499",
  """Remove after merging SER-499. This is only here for backwards-compatibility with all model
     classes that used to unnecessarily specify both KeyT and T."""
)
trait InsertsAndUpdatesWithKey[KeyT, T <: KeyedEntity[KeyT]] extends InsertsAndUpdates[T]


trait CanInsertAndUpdateThroughServices[T <: KeyedEntity[_]] { this: T =>
  def services: InsertsAndUpdates[T]

  def insert(): T = services.insert(this)
  def update(): T = services.update(this)
}

trait CanInsertAndUpdateAsThroughServices[ModelT <: HasEntity[EntityT], EntityT <: KeyedCaseClass[_]]{
  this: ModelT =>

  def services: InsertsAndUpdatesAsEntity[ModelT, EntityT]
  def insert(): ModelT = services.insert(this)
  def update(): ModelT = services.update(this)
}
