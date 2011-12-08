package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}

class VoiceSampleTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VoiceSample]
with CreatedUpdatedEntityTests[VoiceSample]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VoiceSample] methods
  //
  def newEntity = {
    VoiceSample(s3Location = "", isForEnrollment = true)
  }

  def saveEntity(toSave: VoiceSample) = {
    VoiceSample.save(toSave)
  }

  def restoreEntity(id: Long) = {
    VoiceSample.findById(id)
  }

  override def transformEntity(toTransform: VoiceSample) = {
    toTransform.copy(
      s3Location = "help",
      isForEnrollment = false
    )
  }

}