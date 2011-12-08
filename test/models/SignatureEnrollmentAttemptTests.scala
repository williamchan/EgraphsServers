package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}

class SignatureEnrollmentAttemptTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[SignatureEnrollmentAttempt]
with CreatedUpdatedEntityTests[SignatureEnrollmentAttempt]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[SignatureEnrollmentAttempt] methods
  //
  def newEntity = {
    val celebrity = Celebrity().save()
    SignatureEnrollmentAttempt(celebrityId = celebrity.id, xyzmoProfileId = celebrity.id.toString)
  }

  def saveEntity(toSave: SignatureEnrollmentAttempt) = {
    SignatureEnrollmentAttempt.save(toSave)
  }

  def restoreEntity(id: Long) = {
    SignatureEnrollmentAttempt.findById(id)
  }

  override def transformEntity(toTransform: SignatureEnrollmentAttempt) = {
    toTransform.copy(
      xyzmoEnrollResult = "success"
    )
  }

}