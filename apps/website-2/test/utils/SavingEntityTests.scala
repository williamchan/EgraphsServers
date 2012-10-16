package utils

import org.scalatest.matchers.ShouldMatchers
import org.squeryl.KeyedEntity
import org.scalatest.FlatSpec
import org.apache.commons.lang.RandomStringUtils

trait SavingEntityIdLongTests[T <: KeyedEntity[Long]] extends SavingEntityTests[Long, T] { this: FlatSpec with ShouldMatchers =>
  def newIdValue = 0L
  def improbableIdValue = Integer.MAX_VALUE
}

trait SavingEntityIdStringTests[T <: KeyedEntity[String]] extends SavingEntityTests[String, T] { this: FlatSpec with ShouldMatchers =>
  val newIdValue = RandomStringUtils.randomAlphanumeric(30)
  def improbableIdValue = RandomStringUtils.randomAlphanumeric(30)
}

/**
 * Test set for any class whose companion object implements SavesWithLongKey[TheClass]
 *
 * Ensures that basic CRUD works properly.
 */
trait SavingEntityTests[KeyT, T <: KeyedEntity[KeyT]] { this: FlatSpec with ShouldMatchers =>
  //
  // Abstract Methods
  //
  /** Render a new, non-persisted entity */
  def newEntity: T

  /** Save an entity to the data store */
  def saveEntity(toSave: T): T

  /** Load an entity with the given id from the data store */
  def restoreEntity(id: KeyT): Option[T]

  /** Transform all fields directly managed by the class and its companion. */
  def transformEntity(toTransform: T): T

  def newIdValue: KeyT

  def improbableIdValue: KeyT

  //
  // Test cases
  //
  "A new instance" should "have initial id equal to " + newIdValue in {
    newEntity.id should be === (newIdValue)
  }

  "A restored instance" should "be the same as the originally saved one" in {
    val saved = saveEntity(newEntity)
    val maybeRestored = restoreEntity(saved.id)

    maybeRestored should not be (None)
    maybeRestored.get should be (saved)
  }

  "Restoring an unsaved id" should "return None" in {
    // We can only guarentee that 0 shouldn't be used as an id already if we aren't clearing the database, but max
    // probably isn't either and it makes for a more interesting test.
    restoreEntity(improbableIdValue) should be (None)
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