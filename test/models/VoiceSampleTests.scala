package models

import libs.Blobs
import Blobs.Conversions._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._

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
    VoiceSample(isForEnrollment = true)
  }

  def saveEntity(toSave: VoiceSample) = {
    VoiceSample.save(toSave)
  }

  def restoreEntity(id: Long) = {
    VoiceSample.findById(id)
  }

  override def transformEntity(toTransform: VoiceSample) = {
    toTransform.copy(
      isForEnrollment = false
    )
  }

  it should "save voiceStr to Blobstore" in {
    val saved = VoiceSample(isForEnrollment = true).save(TestConstants.voiceStr)
    Blobs.get(VoiceSample.getJsonUrl(saved.id)).get.asString should be(TestConstants.voiceStr)
  }

}
