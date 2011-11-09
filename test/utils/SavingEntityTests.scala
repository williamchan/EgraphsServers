package utils

import org.scalatest.matchers.ShouldMatchers
import org.squeryl.KeyedEntity
import play.test.UnitFlatSpec

/**
 * Test set for any class whose companion object implements Saves[TheClass]
 *
 * Ensures that basic CRUD works properly.
 */
trait SavingEntityTests[T <: KeyedEntity[Long]] { this: UnitFlatSpec with ShouldMatchers =>
  //
  // Abstract Methods
  //
  /** Render a new, non-persisted entity */
  def newEntity: T

  /** Save an entity to the data store */
  def saveEntity(toSave: T): T

  /** Load an entity with the given id from the data store */
  def restoreEntity(id: Long): Option[T]

  /** Transform all fields directly managed by the class and its companion. */
  def transformEntity(toTransform: T): T
  
  //
  // Test cases
  //
  "A new instance" should "have initial id <= 0" in {
    newEntity.id should be <= (0L)
  }

  "A restored instance" should "be the same as the originally saved one" in {
    val saved = saveEntity(newEntity)
    val maybeRestored = restoreEntity(saved.id)

    maybeRestored should not be (None)
    maybeRestored.get should be (saved)
  }

  "Restoring an unsaved id" should "return None" in {
    restoreEntity(1) should be (None)
  }

  "Restoring an updated instance" should "yield the updated version, not the original" in {
    val inserted = saveEntity(newEntity)
    val updated = saveEntity(transformEntity(inserted))
    val updatedRestored = restoreEntity(updated.id).get

    updated should not be (inserted)
    updatedRestored should be (updated)
    updatedRestored should not be (inserted)
  }
}