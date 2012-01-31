package models

import services.blobs.Blobs
import Blobs.Conversions._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig


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
  val store = AppConfig.instance[SignatureSampleStore]
  val blobs = AppConfig.instance[Blobs]

  def newEntity = {
    SignatureSample(isForEnrollment = true)
  }

  def saveEntity(toSave: SignatureSample) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: SignatureSample) = {
    toTransform.copy(
      isForEnrollment = false
    )
  }

  it should "save signatureStr to Blobstore" in {
    val saved = SignatureSample(isForEnrollment = true).save(TestConstants.signatureStr)
    blobs.get(SignatureSample.getJsonUrl(saved.id)).get.asString should be(TestConstants.signatureStr)
  }

}
