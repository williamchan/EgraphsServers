package services.db

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._

/**
 * A couple utility methods for querying [[org.squeryl.KeyedEntity]]s. This trait is for the
 * original stype of models which are KeyedEntities, as opposed to the new style in which
 * the model takes a separate KeyedEntity type as an argument.
 *
 * Usage:
 * {{{
 *   case class Person(id: Long = 0L, name: String = "", created: Date = new Date(0L))
 *
 *   object Person extends InsertsAndUpdates[Person] with Querying[Long, Person] {
 *     override val table: Table[Person] = MySquerylDb.people
 *
 *     def testInsertAndQuery {
 *       val inserted = insert(Person())
 *
 *       assert(findById(inserted.id).get.id == inserted.id) // passes
 *     }
 *   }
 * }}}
 */
trait QueriesAsModel[KeyT, ModelT <: KeyedEntity[KeyT]] {
  /**
   * Locates an object by its id.
   *
   * @param id the id of the object to locate
   *
   * @return the located object or None
   */
  def findById(id: KeyT): Option[ModelT]= {
    table.lookup(id)
  }

  /**
   * Gets an object by its id, throws an exception if not found.
   *
   * @param id the id of the object to locate
   *
   * @return the located object
   *
   * @throws a RuntimeException with ID information if it failed to find the entity.
   */
  def get(id: KeyT)(implicit m: Manifest[ModelT]): ModelT = {
    findById(id).getOrElse(
      throw new RuntimeException(
        "DB contained no instances of class " + m.erasure.getName + " with id="+id
      )
    )
  }

  //
  // Abstract members
  //
  protected def table: Table[ModelT]
}
