package utils

import models.HasCreatedUpdated
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.squeryl.KeyedEntity
import services.Time.{defaultTimestamp, now}

/**
 * Mix this in with test cases for any entity that uses the HasCreatedUpdated
 * and SavesCreatedUpdated to ensure that the behavior is correct
 */
trait CreatedUpdatedEntityTests[KeyT, T <: HasCreatedUpdated with KeyedEntity[KeyT]] {
  this: FlatSpec with ShouldMatchers with DateShouldMatchers with SavingEntityTests[KeyT, T] =>

  //
  // Test cases
  //
  "A new instance" should "start with default timestamp" in {
    val entity = newEntity
    
    entity.created should be (defaultTimestamp)
    entity.updated should be (defaultTimestamp)
  }

  "An inserted instance" should "have both timestamps set" in {
    val saved = saveEntity(newEntity)
    val restored = restoreEntity(saved.id).get

    saved.created.getTime should be (now.getTime plusOrMinus (100 milliseconds))
    saved.created should be (saved.updated)

    saved.created should be (restored.created)
    saved.updated should be (restored.updated)
  }

  "An updated instance" should "have only the 'updated' field altered" in {
    // Set up
    val inserted = saveEntity(newEntity)
    val sleepDuration = 1 millisecond

    Thread.sleep(sleepDuration)

    // Run test
    val updated = saveEntity(inserted)
    val restored = restoreEntity(updated.id).get

    // Check expectations
    updated.created should be (inserted.created)
    updated.updated.getTime should be >= (inserted.updated.getTime + sleepDuration)
    restored.created should be (updated.created)
    restored.updated should be (updated.updated)
  }
}