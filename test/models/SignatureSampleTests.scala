package models

import libs.Blobs
import Blobs.Conversions._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._


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
    SignatureSample(isForEnrollment = true)
  }

  def saveEntity(toSave: SignatureSample) = {
    SignatureSample.save(toSave)
  }

  def restoreEntity(id: Long) = {
    SignatureSample.findById(id)
  }

  override def transformEntity(toTransform: SignatureSample) = {
    toTransform.copy(
      isForEnrollment = false
    )
  }

  it should "save signatureStr to Blobstore" in {
    val saved = SignatureSample(isForEnrollment = true).save(TestConstants.signatureStr)
    Blobs.get("signaturesamples/" + saved.id).get.asString should be(TestConstants.signatureStr)
  }

}
