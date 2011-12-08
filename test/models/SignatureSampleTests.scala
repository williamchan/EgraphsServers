package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}

class SignatureSampleTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[SignatureSample]
with CreatedUpdatedEntityTests[SignatureSample]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[SignatureSample] methods
  //
  def newEntity = {
    SignatureSample(s3Location = "", isForEnrollment = true)
  }

  def saveEntity(toSave: SignatureSample) = {
    SignatureSample.save(toSave)
  }

  def restoreEntity(id: Long) = {
    SignatureSample.findById(id)
  }

  override def transformEntity(toTransform: SignatureSample) = {
    toTransform.copy(
      s3Location = "help",
      isForEnrollment = false
    )
  }

}