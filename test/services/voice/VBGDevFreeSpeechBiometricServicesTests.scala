package services.voice

import play.Play
import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import services.blobs.Blobs
import javax.sound.sampled.{AudioInputStream, AudioFileFormat, AudioSystem}
import collection.immutable.IndexedSeq
import java.io.{File, ByteArrayOutputStream}

class VBGDevFreeSpeechBiometricServicesTests extends UnitFlatSpec with ShouldMatchers {

//    "vbg" should "generate two enrollment samples as WAVs" in {
////      val megawillAudioLocations: List[String] = getAudioFileLocations("test/files/megawill", numFiles = 2)
////      val megawillAudioFiles = getAudioFilesFromFileLocations(megawillAudioLocations)
////      val megawillAudioStitched : Option[AudioInputStream] = VBGDevFreeSpeechBiometricServices.stitchWAVs(megawillAudioFiles)
////      val megastream: AudioInputStream = megawillAudioStitched.get
////      AudioSystem.write(megastream, AudioFileFormat.Type.WAVE, new File("tmp/blobstore/egraphs-test/megasample.wav"))
//
//      val will1AudioLocations: List[String] = getAudioFileLocations("test/files/will1")
//      val will1AudioFiles = getAudioFilesFromFileLocations(will1AudioLocations)
//      val will1AudioStitched : Option[AudioInputStream] = VBGDevFreeSpeechBiometricServices.stitchWAVs(will1AudioFiles)
//      val stream1: AudioInputStream = will1AudioStitched.get
//      AudioSystem.write(stream1, AudioFileFormat.Type.WAVE, new File("tmp/blobstore/egraphs-test/sample1.wav"))
//      val will2AudioLocations: List[String] = getAudioFileLocations("test/files/will2")
//      val will2AudioFiles = getAudioFilesFromFileLocations(will2AudioLocations)
//      val will2AudioStitched : Option[AudioInputStream] = VBGDevFreeSpeechBiometricServices.stitchWAVs(will2AudioFiles)
//      val stream2: AudioInputStream = will2AudioStitched.get
//      AudioSystem.write(stream2, AudioFileFormat.Type.WAVE, new File("tmp/blobstore/egraphs-test/sample2.wav"))
//    }

//  "vbg" should "enroll" in {
//    val startEnrollmentRequest: VBGRequest = VBGDevFreeSpeechBiometricServices.sendStartEnrollmentRequest("will@egraphs.com", true)
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
//    val enrollUserRequest: VBGRequest = VBGDevFreeSpeechBiometricServices.sendEnrollUserRequest(transactionId)
//    println(enrollUserRequest.getResponseValue(VBGRequest._errorCode))
//    val successValue = enrollUserRequest.getResponseValue(VBGRequest._success)
//    println(successValue)
//
//    val finishRequest: VBGRequest = VBGDevFreeSpeechBiometricServices.sendFinishEnrollTransactionRequest(transactionId, successValue)
//    println(finishRequest.getResponseValue(VBGRequest._errorCode))
//  }

//  private def sendAudioCheckRequest(transactionId: String, sampleFilename: String) {
//    val checkRequest1: VBGRequest = VBGDevFreeSpeechBiometricServices.sendAudioCheckRequest(transactionId.toLong, sampleFilename)
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
//    val startVerificationRequest: VBGRequest = VBGDevFreeSpeechBiometricServices.sendStartVerificationRequest("will@egraphs.com")
//    println("startVerificationRequest errorcode " + startVerificationRequest.getResponseValue(VBGRequest._errorCode))
//    val transactionId: String = startVerificationRequest.getResponseValue(VBGRequest._transactionId)
//    println("transactionId " + transactionId)
//
//    val verifySampleRequest: VBGRequest = VBGDevFreeSpeechBiometricServices.sendVerifySampleRequest(transactionId, wavBinary)
//    println("verifySample errorcode " + verifySampleRequest.getResponseValue(VBGRequest._errorCode))
//    val successValue: String = verifySampleRequest.getResponseValue(VBGRequest._success)
//    val score: String = verifySampleRequest.getResponseValue(VBGRequest._score)
//    val usableTime: String = verifySampleRequest.getResponseValue(VBGRequest._usableTime)
//    println(List(successValue, score, usableTime).mkString(", "))
//
//    val finishRequest: VBGRequest = VBGDevFreeSpeechBiometricServices.sendFinishVerifyTransactionRequest(transactionId, successValue, score)
//    println("finishRequest errorcode " + finishRequest.getResponseValue(VBGRequest._errorCode))
//  }

  // ================= helpers

  private def getAudioFilesFromFileLocations(will1AudioLocations: scala.List[String]): List[Array[Byte]] = {
    for (audioLocation <- will1AudioLocations) yield getVoiceSampleBinary(audioLocation)
  }

  private def getAudioFileLocations(base: String, numFiles: Int = 10): List[String] = {
    val seq: IndexedSeq[String] = for (i <- 1 until (numFiles+1)) yield {
      base + "/audio" + i + ".wav"
    }
    seq.toList
  }

  private def convertAudioInputStreamToByteArray(audioInputStream: AudioInputStream): Array[Byte] = {
    val bas: ByteArrayOutputStream = new ByteArrayOutputStream()
    AudioSystem.write(audioInputStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, bas)
    bas.toByteArray
  }

  private def getVoiceSampleBinary(filename: String): Array[Byte] = {
    val file = Play.getFile(filename)
    Blobs.Conversions.fileToByteArray(file)
  }

}
