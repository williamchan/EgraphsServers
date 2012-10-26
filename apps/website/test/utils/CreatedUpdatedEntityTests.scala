package utils

import models.HasCreatedUpdated
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.squeryl.KeyedEntity
import services.Time.{defaultTimestamp, now}
import java.sql.Timestamp

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
    
    timestampsShouldMatch(entity.created, defaultTimestamp)
    timestampsShouldMatch(entity.updated, defaultTimestamp)
  }

  "An inserted instance" should "have both timestamps set" in {
    val saved = saveEntity(newEntity)
    val restored = restoreEntity(saved.id).get

    saved.created.getTime should be (now.getTime plusOrMinus (100 milliseconds))
    timestampsShouldMatch(saved.created, saved.updated)

    timestampsShouldMatch(saved.created, restored.created)
    timestampsShouldMatch(saved.updated, restored.updated)
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
    timestampsShouldMatch(updated.created, inserted.created)
    updated.updated.getTime should be >= (inserted.updated.getTime + sleepDuration)
    timestampsShouldMatch(restored.created, updated.created)
    timestampsShouldMatch(restored.updated, updated.updated)
  }

  //
  // Private members
  //
  private def timestampsShouldMatch(ts1: Timestamp, ts2: Timestamp) {
    ts1.getTime should be (ts2.getTime)
  }
}