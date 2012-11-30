package services.db

import org.squeryl.PrimitiveTypeMode.__thisDsl
import org.squeryl.KeyedEntity
import org.squeryl.Table

/**
 * Gives an object the ability to delete instances of the associated type, eg Person.delete(personInstance).
 * Similar usage to Saves, see that for more info.
 */
trait Deletes[KeyT, T <: KeyedEntity[KeyT]] {
  /**The table that manages this entity in services.db.Schema  */
  protected def table: Table[T]

  /**
   * Deletes a persisted object.
   *
   * @param toDelete the object to delete
   */
  def delete(toDelete: T): Unit = {
    table.delete(toDelete.id)
  }
}