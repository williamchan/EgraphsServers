package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}

class VoiceEnrollmentAttemptTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VoiceEnrollmentAttempt]
with CreatedUpdatedEntityTests[VoiceEnrollmentAttempt]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VoiceEnrollmentAttempt] methods
  //
  def newEntity = {
    val celebrity = Celebrity().save()
    VoiceEnrollmentAttempt(celebrityId = celebrity.id)
  }

  def saveEntity(toSave: VoiceEnrollmentAttempt) = {
    VoiceEnrollmentAttempt.save(toSave)
  }

  def restoreEntity(id: Long) = {
    VoiceEnrollmentAttempt.findById(id)
  }

  override def transformEntity(toTransform: VoiceEnrollmentAttempt) = {
    toTransform.copy(
      vbgTransactionId = 0L,
      vbgStatus = 0L
    )
  }

}