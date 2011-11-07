import models.HasCreatedUpdated
import org.scalatest.matchers.ShouldMatchers
import org.squeryl.KeyedEntity
import play.test.UnitFlatSpec
import libs.Time.{defaultTimestamp, now}

/**
 * Mix this in with test cases for any entity that uses the HasCreatedUpdated
 * and SavesCreatedUpdated to ensure that the behavior is correct
 */
trait CreatedUpdatedEntityTests[T <: HasCreatedUpdated with KeyedEntity[Long]] {
  this: UnitFlatSpec with ShouldMatchers with SavingEntityTests[T] =>

  //
  // Test cases
  //
  "A new instance" should "start with default timestamp" in {
    newEntity.created should be (defaultTimestamp)
    newEntity.updated should be (defaultTimestamp)
  }

  "An inserted instance" should "have both timestamps set" in {
    val saved = saveEntity(newEntity)
    val restored = restoreEntity(saved.id).get

    saved.created.getTime should be (now.getTime plusOrMinus 100)
    saved.created should be (saved.updated)

    saved.created should be (restored.created)
    saved.updated should be (restored.updated)
  }

  "An updated instance" should "have only the 'updated' field altered" in {
    // Set up
    val inserted = saveEntity(newEntity)
    val sleepDuration = 20L

    Thread.sleep(sleepDuration)

    // Run test
    val updated = saveEntity(inserted)
    val restored = restoreEntity(updated.id).get

    // def getTuple(entity: T) = (entity.created, entity.updated)

    /*println("inserted" + getTuple(inserted))
    println("updated " + getTuple(updated))
    println("restored" + getTuple(restored))*/

    // Check expectations
    updated.created should be (inserted.created)
    updated.updated.getTime should be >= (inserted.updated.getTime + sleepDuration)
    restored.created should be (updated.created)
    restored.updated should be (updated.updated)
  }
}