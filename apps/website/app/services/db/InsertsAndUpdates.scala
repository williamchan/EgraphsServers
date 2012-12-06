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
 * @tparam KeyT the entity's key type
 * @tparam T the entity's type
 */
trait InsertsAndUpdates[KeyT, T <: KeyedEntity[KeyT]] extends InsertAndUpdateHooks[T] {
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
