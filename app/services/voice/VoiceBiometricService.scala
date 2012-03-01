package services.voice

import models.{VoiceSample, EnrollmentBatch, Egraph}


trait VoiceBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch, voiceSamples: scala.List[VoiceSample]): Either[VoiceBiometricsError, Boolean]
  def verify(audio: Array[Byte], egraph: Egraph): Either[VoiceBiometricsError, VoiceVerificationResult]
}

// TODO: Write an integration test against VBG service to ensure our code works.
/**
 * VBG implementation of voice biometric services.
 */
class VBGVoiceBiometricService extends VoiceBiometricService {
  val vbg = VBGDevFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch, voiceSamples: scala.List[VoiceSample]): Either[VoiceBiometricsError, Boolean] = {
    vbg.enroll(enrollmentBatch, voiceSamples)
  }

  def verify(audio: Array[Byte], egraph: Egraph): Either[VoiceBiometricsError, VoiceVerificationResult] = {
    vbg.verify(audio, egraph)
  }
}

class YesMaamVoiceBiometricService extends VoiceBiometricService {

  def enroll(enrollmentBatch: EnrollmentBatch, voiceSamples: scala.List[VoiceSample]) = Right(true)

  def verify(audio: Array[Byte], egraph: Egraph) = {
    Right(new VoiceVerificationResult {
      val score: Long = 100
      val success = true
    })
  }
}