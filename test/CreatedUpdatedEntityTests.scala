import models.HasCreatedUpdated
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import libs.Time.{defaultTimestamp, now}

/**
 * Mix this in with test cases for any entity that uses the HasCreatedUpdated
 * and SavesCreatedUpdated to ensure that the behavior is correct
 */
trait CreatedUpdatedEntityTests[T <: HasCreatedUpdated] { this: UnitFlatSpec with ShouldMatchers =>

  //
  // Abstract members
  //

  /** Provide an instance of the entity type under test */
  def getCreatedUpdatedEntity: T

  /**
   *  Persist an instance of the entity type under test and return the
   *  persisted version.
   */
  def saveCreatedUpdated(entity: T): T

  //
  // Test cases
  //
  "Any created/updated entity" should "start out with default timestamp" in {
    getCreatedUpdatedEntity.created should be (defaultTimestamp)
    getCreatedUpdatedEntity.updated should be (defaultTimestamp)
  }

  it should "update both timestamps on insert" in {
    val saved = saveCreatedUpdated(getCreatedUpdatedEntity)

    saved.created.getTime should be (now.getTime plusOrMinus 100)
    saved.created should be (saved.updated)
  }

  it should "update only updated on update" in {
    // Set up
    val insertedEntity = saveCreatedUpdated(getCreatedUpdatedEntity)
    val sleepDuration = 20L

    Thread.sleep(sleepDuration)
    
    // Run test
    val updatedEntity = saveCreatedUpdated(insertedEntity)

    // Check expectations
    updatedEntity.created should be (insertedEntity.created)
    updatedEntity.updated.getTime should be >= (insertedEntity.updated.getTime + sleepDuration)
  }
}