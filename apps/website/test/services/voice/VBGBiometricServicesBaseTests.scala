package services.voice

import models._
import utils._
import play.api.Play
import services.blobs.Blobs
import org.apache.commons.codec.binary.Base64
import javax.sound.sampled.{AudioInputStream, AudioFileFormat, AudioSystem}
import models.{EnrollmentSample, EnrollmentBatch}
import Blobs.Conversions._
import models.Egraph
import scala.Some
import vbg.VBGVerifySample
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * IMPORTANT! -- Do not write tests for VBGProdFreeSpeechBiometricServices or VBGBetaFreeSpeechBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on VBG.
 * Instead, VBGTestFreeSpeechBiometricServices exists for automated tests of celebrity-fs-en account.
 */
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VBGBiometricServicesBaseTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with DBTransactionPerTest
{

  implicit private def blobs = services.AppConfig.instance[Blobs]

  "enroll" should "call saveCombinedWavToBlobStore" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
    val voiceStr = TestConstants.voiceStr()
    new EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save("", voiceStr = voiceStr)
    MockVBGBiometricServices.enroll(enrollmentBatch)
    (enrollmentBatch.services.blobs.get(EnrollmentBatch.getCombinedWavUrl(enrollmentBatch.id)).get.asByteArray.length > 0) should be(true)
  }

  "convertWavTo8kHzBase64" should "convert to 8khz encoded in base64" in {
    val wavBinary_44kHz: Array[Byte] = getVoiceSampleBinary("44khz.wav")
    val wav_8kHz_base64: String = MockVBGBiometricServices.convertWavTo8kHzBase64(wavBinary_44kHz)
    wav_8kHz_base64 should be(TestConstants.voiceStr_8khz())
  }

  "convertWavTo8kHzBase64" should "handle zero case" in {
    MockVBGBiometricServices.convertWavTo8kHzBase64(new Array[Byte](0)) should be("")
  }

  "stitchWAVs" should "stitch multiple WAVs together" in new EgraphsTestApplication {
    val filename = "44khz.wav"
    val targetFile = "stitched_3x.wav"
    val resultFile = resourceFile(targetFile)

    val appendedFiles: Option[AudioInputStream] = MockVBGBiometricServices.stitchWAVs(List(getVoiceSampleBinary(filename), getVoiceSampleBinary(filename), getVoiceSampleBinary(filename)))
    AudioSystem.write(appendedFiles.get, AudioFileFormat.Type.WAVE, resultFile)
    resourceFile(targetFile).length() should be(921166)
  }

  "stitchWAVs" should "handle base cases" in new EgraphsTestApplication {
    MockVBGBiometricServices.stitchWAVs(List()) should be(None)

    val filename = "44khz.wav"
    val result: AudioInputStream = MockVBGBiometricServices.stitchWAVs(List(getVoiceSampleBinary(filename))).get
    val audioISFromFile: AudioInputStream = AudioSystem.getAudioInputStream(resourceFile(filename))
    Base64.encodeBase64(MockVBGBiometricServices.convertAudioInputStreamToByteArray(result)) should be(Base64.encodeBase64(MockVBGBiometricServices.convertAudioInputStreamToByteArray(audioISFromFile)))
  }

  "getUserId" should "prepend _userIdPrefix" in {
    MockVBGBiometricServices.getUserId(1L) should be("mock1")
    VBGDevRandomNumberBiometricServices.getUserId(celebrityId = 1L) should be("dev1")
    VBGDevFreeSpeechBiometricServices.getUserId(celebrityId = 1L) should be("dev1")
    VBGProdFreeSpeechBiometricServices.getUserId(celebrityId = 1L) should be("prod1")
    VBGBetaFreeSpeechBiometricServices.getUserId(celebrityId = 1L) should be("beta1")
    VBGTestFreeSpeechBiometricServices.getUserId(celebrityId = 1L) should be("test1")
  }

  // TODO: PLAY20 migration. This test now fails. Wizzle, can you take a look at it?
  "VBGDevFreeSpeechBiometricServices" should "test end-to-end" in (pending) /*{
    testVBGEnrollAndVerify(VBGDevFreeSpeechBiometricServices)
  }*/

  /**
   * This test costs $0.40 per run.  We shouldn't run this test regularly.
   */
//  "VBGTestFreeSpeechBiometricServices" should "test end-to-end" in {
//    testVBGEnrollAndVerify(VBGTestFreeSpeechBiometricServices)
//  }

  private def testVBGEnrollAndVerify(vbg: VBGBiometricServicesBase)(implicit blobs: Blobs) {
    val enroll1: Array[Byte] = blobs.getStaticResource("test-files/vbg/enroll1.wav").get.asByteArray
    val enroll2: Array[Byte] = blobs.getStaticResource("test-files/vbg/enroll2.wav").get.asByteArray
    val verifyTrue: Array[Byte] = blobs.getStaticResource("test-files/vbg/verify_true.wav").get.asByteArray
    val verifyFalse: Array[Byte] = blobs.getStaticResource("test-files/vbg/verify_false.wav").get.asByteArray

    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity))
    val order = customer.buy(product).save()

    val enrollmentBatch: EnrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = TestConstants.shortWritingStr, voiceStr = Base64.encodeBase64String(enroll1))
    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = TestConstants.shortWritingStr, voiceStr = Base64.encodeBase64String(enroll2))
    val enrollResult: Either[VoiceBiometricsError, Boolean] = vbg.enroll(enrollmentBatch)
    enrollResult.right.get should be(true)

    val egraphVerifyTrue: Egraph = Egraph(orderId = order.id).withAssets(signature = TestConstants.shortWritingStr, message = None, audio = verifyTrue).save()
    val verifyResultTrue: Either[VoiceBiometricsError, VBGVerifySample] = vbg.verify(egraphVerifyTrue)
    verifyResultTrue.right.get.success.get should be(true)

    val egraphVerifyFalse: Egraph = Egraph(orderId = order.id).withAssets(signature = TestConstants.shortWritingStr, message = None, audio = verifyFalse).save()
    val verifyResultFalse: Either[VoiceBiometricsError, VBGVerifySample] = vbg.verify(egraphVerifyFalse)
    verifyResultFalse.right.get.success.get should be(false)
  }

  private def getVoiceSampleBinary(filename: String): Array[Byte] = {
    val file = new EgraphsTestApplication {
      val file = resourceFile(filename) 
    }.file
    Blobs.Conversions.fileToByteArray(file)
  }
}
