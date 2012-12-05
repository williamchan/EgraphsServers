package services.db

import org.squeryl.PrimitiveTypeMode.__thisDsl
import org.squeryl.KeyedEntity
import org.squeryl.Table

/**
 * Gives an object the ability to delete instances of the associated type, eg Person.delete(personInstance).
 * Similar usage to Saves, see that for more info.
 *
 * VIRTUALLY NO MODELS SHOULD USE THIS. Exceptions are made for tags. Reasons why we do not typically delete
 * database records include it is easy to ruin referential integrity in a relational database. A thorough
 * discussion is available at http://serverfault.com/questions/31455/should-i-ever-delete-sql-and-db-anything
 */
trait Deletes[KeyT, T <: KeyedEntity[KeyT]] {
  /**The table that manages this entity in services.db.Schema  */
  protected def table: Table[T]

  /**
   * Deletes a persisted object.
   *
   * @param toDelete the object to delete
   */
  def delete (toDelete: T) {
    table.delete(toDelete.id)
  }
}