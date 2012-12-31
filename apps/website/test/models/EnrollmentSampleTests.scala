package models

import egraphs.playutils.Encodings.Base64
import utils._
import services.AppConfig
import services.blobs.Blobs
import Blobs.Conversions._
import play.api.Play

class EnrollmentSampleTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[EnrollmentSample]
  with CreatedUpdatedEntityTests[Long, EnrollmentSample]
  with DateShouldMatchers
  with DBTransactionPerTest {
  //
  // SavingEntityTests[EnrollmentSample] methods
  //

  private def store = AppConfig.instance[EnrollmentSampleStore]
  private def blobs = AppConfig.instance[Blobs]

  def newEntity = {
    val celebrity = TestData.newSavedCelebrity()
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
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
    toTransform.copy(
      // Actually, not much to test here. Just providing something here for now.
      enrollmentBatchId = enrollmentBatch.id
    )
  }

  "save" should "save signatureStr and voiceStr to Blobstore" in new EgraphsTestApplication {
    val signatureStr = TestConstants.shortWritingStr
    val voiceStr = TestConstants.fakeAudioStr()
    val saved = newEntity.save(signatureStr = signatureStr, voiceStr = voiceStr)

    saved.getSignatureJson should be(signatureStr)
    Base64.encode(saved.getWav) should be (voiceStr)
  }
  
  "getSignatureJson and getWav" should "handle edge cases gracefully" in new EgraphsTestApplication {
    val saved = newEntity.save()
    clearOldBlobData(saved)

    saved.getSignatureJson should be("")
    saved.getWav should be (new Array[Byte](0))
  }

  "putSignatureXml" should "save to blobstore at SignatureXmlURL" in new EgraphsTestApplication {
    val xyzmoSignatureDataContainer = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature1.xml"))

    val saved = newEntity.save()
    clearOldBlobData(saved)
    
    saved.putSignatureXml(xyzmoSignatureDataContainer = xyzmoSignatureDataContainer)
    saved.services.blobs.get(EnrollmentSample.getSignatureXmlUrl(saved.id)).get.asString should be (xyzmoSignatureDataContainer)
  }

  /**
   * Just delete the old blob data before using sample in a test since it may have data from a previous run that wasn't cleared.
   */
  def clearOldBlobData(sample: EnrollmentSample) {
    blobs.delete(EnrollmentSample.getSignatureJsonUrl(sample.id))
    blobs.delete(EnrollmentSample.getWavUrl(sample.id))
  }
}
