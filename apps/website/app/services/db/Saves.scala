package services.db

import org.squeryl.{KeyedEntity,Table}
import org.squeryl.dsl.ast.{LogicalBoolean, EqualityExpression, UpdateAssignment}
import org.squeryl.PrimitiveTypeMode._
import sun.security.krb5.internal.ktab.KeyTab

// TODO(SER-499): Is this usage up to date? Check others as well.
/**
 * Gives an object the ability to save instances of the associated type, eg Person.save(personInstance).
 * Also, provides hooks to transform the entity before saving it.
 *
 * This trait assumes any entity with id of <= 0 does not exist in the database and will attempt to insert it rather
 * than update, so make sure 0 or a negative number is your sentinel value! (IDs in databases typically start from 1.)
 *
 * Usage:
 * {{{
 *    case class Person(id: Long = 0L, name: String = "") extends KeyedEntity[Long]
 *
 *    object Person extends SavesWithLongKey[Person] {
 *       override val table = MySquerylDb.people
 * }
 *
 *    // Later on in application code...
 *    Person.save(Person(name="Jonesy"))
 *
 * }}}
 *
 */
trait SavesWithLongKey[T <: KeyedEntity[Long]] extends Saves[Long, T] {
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

trait Saves[KeyT, T <: KeyedEntity[KeyT]]
  extends InsertsAndUpdates[T]
  with Queries[KeyT, T]
  with InsertAndUpdateHooks[T]
{

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


  //
  // Private API
  //
  protected def updateTable(toUpdate: T): T = {
    val finalEntity = performTransforms(preUpdateTransforms, toUpdate)

    table.update(finalEntity)

    finalEntity
  }

  private def performTransforms(transforms: Seq[(T) => T], entityToTransform: T): T = {
    transforms.foldLeft(entityToTransform)((currEntity, nextTransform) => nextTransform(currEntity))
  }
}