package services.voice

import play.Play
import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import services.blobs.Blobs
import models.vbg.VBGVerifySample
import services.blobs.Blobs.Conversions._
import org.scalatest.BeforeAndAfterEach
import models._
import utils.{TestData, TestConstants, ClearsDatabaseAndValidationAfter, DBTransactionPerTest}
import play.libs.Codec

class VBGDevFreeSpeechBiometricServicesTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {

  private val blobs = services.AppConfig.instance[Blobs]
//  private val vbg = VBGProdFreeSpeechBiometricServices
  private val vbg = VBGDevFreeSpeechBiometricServices

  "vbg" should "test end-to-end" in {
    val enroll1: Array[Byte] = blobs.getStaticResource("test-files/vbg/enroll1.wav").get.asByteArray
    val enroll2: Array[Byte] = blobs.getStaticResource("test-files/vbg/enroll2.wav").get.asByteArray
    val verifyTrue: Array[Byte] = blobs.getStaticResource("test-files/vbg/verify_true.wav").get.asByteArray
    val verifyFalse: Array[Byte] = blobs.getStaticResource("test-files/vbg/verify_false.wav").get.asByteArray

    val celebrity = Celebrity().save()
    val enrollmentBatch: EnrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = TestConstants.signatureStr, voiceStr = Codec.encodeBASE64(enroll1))
    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = TestConstants.signatureStr, voiceStr = Codec.encodeBASE64(enroll2))
    val enrollResult: Either[VoiceBiometricsError, Boolean] = vbg.enroll(enrollmentBatch)
    enrollResult.right.get should be(true)

    val customer = TestData.newSavedCustomer()
    val product = celebrity.newProduct.save()
    val order = customer.buy(product).save()

    val egraphVerifyTrue: Egraph = Egraph(orderId = order.id).withAssets(signature = TestConstants.signatureStr, message = None, audio = verifyTrue).save()
    val verifyResultTrue: Either[VoiceBiometricsError, VBGVerifySample] = vbg.verify(egraphVerifyTrue: Egraph)
    verifyResultTrue.right.get.success.get should be(true)

    val egraphVerifyFalse: Egraph = Egraph(orderId = order.id).withAssets(signature = TestConstants.signatureStr, message = None, audio = verifyFalse).save()
    val verifyResultFalse: Either[VoiceBiometricsError, VBGVerifySample] = vbg.verify(egraphVerifyFalse: Egraph)
    verifyResultFalse.right.get.success.get should be(false)
  }

  //  "vbg" should "enroll" in {
  //    val startEnrollmentRequest: VBGRequest = vbg.sendStartEnrollmentRequest("will@egraphs.com", true)
  //    val transactionId: String = startEnrollmentRequest.getResponseValue(VBGRequest._transactionId)
  //    println("enrollment transactionId " + transactionId)
  //
  ////  Enrollment with 1 sample:
  ////    sendAudioCheckRequest(transactionId, "megasample.wav")
  //
  ////  Enrollment with 1 sample with Feeny mixed in... Successfully enrolls still!
  ////    sendAudioCheckRequest(transactionId, "noisy_megasample.wav")
  //
  ////  Enrolling with 2 samples works
  ////    sendAudioCheckRequest(transactionId, "sample1.wav")
  ////    sendAudioCheckRequest(transactionId, "sample2.wav")
  //
  ////    Trying to enroll with 20 separate samples:
  ////    sendAudioCheckRequest(transactionId, "audio1.wav")
  ////    sendAudioCheckRequest(transactionId, "audio2.wav")
  ////    sendAudioCheckRequest(transactionId, "audio3.wav")
  ////    sendAudioCheckRequest(transactionId, "audio4.wav")
  ////    sendAudioCheckRequest(transactionId, "audio5.wav")
  ////    sendAudioCheckRequest(transactionId, "audio6.wav")
  ////    sendAudioCheckRequest(transactionId, "audio7.wav")
  ////    sendAudioCheckRequest(transactionId, "audio8.wav")
  ////    sendAudioCheckRequest(transactionId, "audio9.wav")
  ////    sendAudioCheckRequest(transactionId, "audio10.wav")
  ////    sendAudioCheckRequest(transactionId, "audio1 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio2 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio3 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio4 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio5 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio6 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio7 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio8 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio9 copy.wav")
  ////    sendAudioCheckRequest(transactionId, "audio10 copy.wav")
  //
  //    val enrollUserRequest: VBGRequest = vbg.sendEnrollUserRequest(transactionId)
  //    println(enrollUserRequest.getResponseValue(VBGRequest._errorCode))
  //    val successValue = enrollUserRequest.getResponseValue(VBGRequest._success)
  //    println(successValue)
  //
  //    val finishRequest: VBGRequest = vbg.sendFinishEnrollTransactionRequest(transactionId, successValue)
  //    println(finishRequest.getResponseValue(VBGRequest._errorCode))
  //  }

  //  private def sendAudioCheckRequest(transactionId: String, sampleFilename: String) {
  //    val checkRequest1: VBGRequest = vbg.sendAudioCheckRequest(transactionId.toLong, sampleFilename)
  //    println(checkRequest1.getResponseValue(VBGRequest._errorCode))
  //    println(checkRequest1.getResponseValue(VBGRequest._usableTime))
  //  }

  //  "vbg" should "verify" in {
  //    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify1.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify2.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify3.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify4.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify5.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify6.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify7.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify8.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify9.wav")
  ////    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify10.wav")
  //
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify11.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify12.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify13.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify14.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify15.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify16.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify17.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify18.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify19.wav")
  ////        val wavBinary: Array[Byte] = getVoiceSampleBinary("test/verify20.wav")
  //
  //    val startVerificationRequest: VBGRequest = vbg.sendStartVerificationRequest("will@egraphs.com")
  //    println("startVerificationRequest errorcode " + startVerificationRequest.getResponseValue(VBGRequest._errorCode))
  //    val transactionId: String = startVerificationRequest.getResponseValue(VBGRequest._transactionId)
  //    println("transactionId " + transactionId)
  //
  //    val verifySampleRequest: VBGRequest = vbg.sendVerifySampleRequest(transactionId, wavBinary)
  //    println("verifySample errorcode " + verifySampleRequest.getResponseValue(VBGRequest._errorCode))
  //    val successValue: String = verifySampleRequest.getResponseValue(VBGRequest._success)
  //    val score: String = verifySampleRequest.getResponseValue(VBGRequest._score)
  //    val usableTime: String = verifySampleRequest.getResponseValue(VBGRequest._usableTime)
  //    println(List(successValue, score, usableTime).mkString(", "))
  //
  //    val finishRequest: VBGRequest = vbg.sendFinishVerifyTransactionRequest(transactionId, successValue, score)
  //    println("finishRequest errorcode " + finishRequest.getResponseValue(VBGRequest._errorCode))
  //  }

  // ================= helpers

  private def getVoiceSampleBinary(filename: String): Array[Byte] = {
    val file = Play.getFile(filename)
    Blobs.Conversions.fileToByteArray(file)
  }

}
