package models

import services.blobs.Blobs
import Blobs.Conversions._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import play.libs.Codec
import services.AppConfig

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
  val store = AppConfig.instance[VoiceSampleStore]
  val blobs = AppConfig.instance[Blobs]

  def newEntity = {
    VoiceSample(isForEnrollment = true)
  }

  def saveEntity(toSave: VoiceSample) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VoiceSample) = {
    toTransform.copy(
      isForEnrollment = false
    )
  }

  it should "save voiceStr to Blobstore" in {
    val saved = VoiceSample(isForEnrollment = true).save(TestConstants.voiceStr())
    val wavFromBlobstore: Array[Byte] = blobs.get(VoiceSample.getWavUrl(saved.id)).get.asByteArray
    Codec.encodeBASE64(wavFromBlobstore) should be(TestConstants.voiceStr())
  }

}
