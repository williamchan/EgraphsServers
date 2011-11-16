package db

import org.squeryl.Table
import org.squeryl.dsl.ast.UpdateAssignment
import org.squeryl.PrimitiveTypeMode._

/**
 * Gives an object the ability to save instances of the parameterized type. Also
 * provides hooks to transform the entity before saving it.
 *
 * This trait assumes any entity with id of <= 0 is unset, and will attempt to insert it rather
 * than update, so make sure 0 or a negative number is your sentinel value!
 *
 * Usage:
 * {{{
 *    case class Person(id: Long = 0L, name: String = "") extends Keyed[Long]
 *
 *    object Person extends Saves[Person] {
 *       override val table = MySquerylDb.people
 *
 *       override def defineUpdate (theOld: person, theNew: person) = {
 *         updateIs(
 *           theOld.name := theNew.name
 *         )
 *       }
 *    }
 *
 *    // Later on in application code...
 *    Person.save(Person(name="Jonesy"))
 *
 * }}}
 *
 * You can also modify the contents either just before an insert or just before an update:
 * {{{
 *   case class Person(id: Long = 0L, name: String = "", created: Date = new Date(0L))
 *
 *   object Person extends Saves[Person] {
 *     beforeInsert((personToTransform) => personToTransform.copy(created=new Date()))
 *
 *     override val table: Table[Person] = MySquerylDb.people
 *
 *     override def defineUpdate (theOld: Person, theNew: Person) {
 *       updateIs(
 *         theOld.name := theNew.name
 *         theOld.version := theNew.version
 *       )
 *     }
 *   }
 * }}}
 */
trait Saves[T <: {def id: Long}] {

  //
  // Abstract members
  //
  /** The table that manages this entity in db.Schema  */
  protected def table: Table[T]

  /**
   * Defines how to update an old row in the database with the new one, using the syntax
   * that usually appears in a Squeryl set() clause. Usually this will be just a matter of taking
   * all the persisted properties and setting them.
   *
   * For example:
   * {{{
   *   case class Fruit(id: Long, name: String) Keyed[Long]
   *
   *   object Fruit extends Saves[Fruit] {
   *     override def defineUpdate(theOld: Fruit, theNew: Fruit) = {
   *        import org.squeryl.PrimitiveTypeMode._
   *        updateIs(
   *          theOld.name := theNew.name
   *        )
   *     }
   *   }
   * }}}
   *
   * @see <a href=http://squeryl.org/inserts-updates-delete.html>Squeryl query documentation</a>
   */
  def defineUpdate(theOld: T, theNew: T): List[UpdateAssignment]

  //
  // Protected API
  //  
  /**
   * Convenience for making a list out of update assignments.
   *
   * @see #defineUpdate
   */
  protected final def updateIs(assignments: UpdateAssignment*): List[UpdateAssignment] = {
    assignments.toList
  }

  //
  // Public API
  //
  /**
   * Persist an object, either by inserting or updating it.
   *
   * Assumes that any object with id <= 0 has not yet been inserted.
   *
   * @param toSave the object to save
   * 
   * @return the final object that was saved, after all transforms
   */
  final def save(toSave: T): T = {
    toSave.id match {
      case n if n <= 0 =>
        insert(toSave)
        
      case _ =>
        updateTable(toSave)
    }
  }

  /**
   * Locates an object by its id.
   *
   * @param id the id of the object to locate
   *
   * @return the located object or None
   */
  final def findById(id: Long): Option[T] = {
    from(table)(row => where(row.id === id) select(row)).headOption
  }

  /**
   * Hook to provide an entity transform that will be applied before inserting any
   * new object.
   *
   * See class documentation for usage.
   */
  final def beforeInsert(transform: (T) => T) {
    preInsertTransforms = preInsertTransforms ++ Vector(transform)
  }

  /**
   * Hook to provide a transform to apply before updating any new object.
   *
   * See class documentation for usage.
   */
  final def beforeUpdate(transform: (T) => T) {
    preUpdateTransforms = preUpdateTransforms ++ Vector(transform)
  }

  //
  // Private API
  //
  private var preInsertTransforms = Vector.empty[(T) => T]
  private var preUpdateTransforms = Vector.empty[(T) => T]

  private def insert(toInsert: T): T = {
    table.insert(performTransforms(preInsertTransforms, toInsert))
  }

  private def updateTable(toUpdate: T): T = {
    val finalEntity = performTransforms(preUpdateTransforms,  toUpdate)
    update(table)(row =>
       where((row.id) === finalEntity.id)
       set(defineUpdate(row, finalEntity):_*)
    )

    finalEntity
  }

  private def performTransforms(transforms: Seq[(T) => T], entityToTransform: T): T = {
    transforms.foldLeft(entityToTransform)((currEntity, nextTransform) => nextTransform(currEntity))
  }
}