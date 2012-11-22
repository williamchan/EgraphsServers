package services.db

import org.squeryl.{KeyedEntity,Table}
import org.squeryl.dsl.ast.{LogicalBoolean, EqualityExpression, UpdateAssignment}
import org.squeryl.PrimitiveTypeMode._
import sun.security.krb5.internal.ktab.KeyTab

/**
 * Gives an object the ability to save instances of the associated type, eg Person.save(personInstance).
 * Also, provides hooks to transform the entity before saving it.
 *
 * This trait assumes any entity with id of <= 0 does not exist in the database and will attempt to insert it rather
 * than update, so make sure 0 or a negative number is your sentinel value! (IDs in databases typically start from 1.)
 *
 * Usage:
 * {{{
 *    case class Person(id: Long = 0L, name: String = "") extends Keyed[Long]
 *
 *    object Person extends SavesWithLongKey[Person] {
 *       override val table = MySquerylDb.people
 *
 *       override def defineUpdate (theOld: person, theNew: person) = {
 *         updateIs(
 *           theOld.name := theNew.name
 *         )
 * }
 * }
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
 *   object Person extends SavesWithLongKey[Person] {
 *     beforeInsert((personToTransform) => personToTransform.copy(created=new Date()))
 *
 *     override val table: Table[Person] = MySquerylDb.people
 *
 *     override def defineUpdate (theOld: Person, theNew: Person) {
 *       updateIs(
 *         theOld.name := theNew.name
 *         theOld.version := theNew.version
 *       )
 * }
 * }
 * }}}
 */
trait SavesWithLongKey[T <: KeyedEntity[Long]] extends Saves[Long, T] {
  override protected final def keysEqual(id: Long, otherId: Long): LogicalBoolean = {
    id === otherId
  }

  override final def save(toSave: T): T = {
    toSave.id match {
      case n if n <= 0 =>
        insert(toSave)

      case _ =>
        updateTable(toSave)
    }
  }
}

trait SavesWithStringKey[T <: KeyedEntity[String]] extends Saves[String, T] {
  override protected final def keysEqual(id: String, otherId: String): LogicalBoolean = {
    id === otherId
  }

  override final def save(toSave: T): T = {
    val maybeSaved = findById(toSave.id)

    maybeSaved match {
      case None =>
        insert(toSave)

      case _ =>
        updateTable(toSave)
    }
  }
}

trait Saves[KeyT, T <: KeyedEntity[KeyT]] extends InsertAndUpdateHooks[T] {

  //
  // Abstract members
  //
  /**The table that manages this entity in services.db.Schema  */
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
   *   object Fruit extends SavesWithLongKey[Fruit] {
   *     override def defineUpdate(theOld: Fruit, theNew: Fruit) = {
   *        import org.squeryl.PrimitiveTypeMode._
   *        updateIs(
   *          theOld.name := theNew.name
   *        )
   * }
   * }
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
  def save(toSave: T): T

  /**
   * Persist an object by inserting it.  Note: This is replaced by SER-499.
   *
   * @param toSave the object to save
   *
   * @return the final object that was saved, after all transforms
   */
  def create(toSave: T): T = {
    insert(toSave)
  }

  /**
   * Locates an object by its id.
   *
   * @param id the id of the object to locate
   *
   * @return the located object or None
   */
  def findById(id: KeyT): Option[T]= {
    from(table)(row => where(keysEqual(row.id, id)) select (row)).headOption
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
  def get(id: KeyT)(implicit m: Manifest[T]): T = {
    findById(id).getOrElse(
      throw new RuntimeException(
        "DB contained no instances of class " + m.erasure.getName + " with id="+id
      )
    )
  }

  protected def keysEqual(id: KeyT, otherId: KeyT): LogicalBoolean

  //
  // Private API
  //
  protected def insert(toInsert: T): T = {
    table.insert(performTransforms(preInsertTransforms, toInsert))
  }

  protected def updateTable(toUpdate: T): T = {
    val finalEntity = performTransforms(preUpdateTransforms, toUpdate)
    update(table)(row =>
      where(keysEqual((row.id), finalEntity.id))
        set (defineUpdate(row, finalEntity): _*)
    )

    finalEntity
  }

  private def performTransforms(transforms: Seq[(T) => T], entityToTransform: T): T = {
    transforms.foldLeft(entityToTransform)((currEntity, nextTransform) => nextTransform(currEntity))
  }
}