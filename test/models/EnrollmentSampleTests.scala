package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.libs.Codec
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import services.blobs.Blobs
import Blobs.Conversions._
import play.Play

class EnrollmentSampleTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[EnrollmentSample]
with CreatedUpdatedEntityTests[EnrollmentSample]
with ClearsDatabaseAndValidationBefore
with DBTransactionPerTest {
  //
  // SavingEntityTests[EnrollmentSample] methods
  //

  val store = AppConfig.instance[EnrollmentSampleStore]

  def newEntity = {
    val celebrity = Celebrity().save()
    val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id)
  }

  def saveEntity(toSave: EnrollmentSample) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: EnrollmentSample) = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = Celebrity().save().id).save()
    toTransform.copy(
      // Actually, not much to test here. Just providing something here for now.
      enrollmentBatchId = enrollmentBatch.id
    )
  }

  "save" should "save signatureStr and voiceStr to Blobstore" in {
    val signatureStr = TestConstants.signatureStr
    val voiceStr = TestConstants.voiceStr()
    val saved = newEntity.save(signatureStr = signatureStr, voiceStr = voiceStr)

    saved.getSignatureJson should be(signatureStr)
    Codec.encodeBASE64(saved.getWav) should be (voiceStr)
  }
  
  "getSignatureJson and getWav" should "handle edge cases gracefully" in {
    val saved = newEntity.save()
    saved.getSignatureJson should be("")
    saved.getWav should be (new Array[Byte](0))
  }

  "putSignatureXml" should "save to blobstore at SignatureXmlURL" in {
    val xyzmoSignatureDataContainer = TestHelpers.getStringFromFile(Play.getFile("test/files/xyzmo_signature1.xml"))

    val saved = newEntity.save()
    saved.putSignatureXml(xyzmoSignatureDataContainer = xyzmoSignatureDataContainer)
    saved.services.blobs.get(EnrollmentSample.getSignatureXmlUrl(saved.id)).get.asString should be (xyzmoSignatureDataContainer)
  }
}