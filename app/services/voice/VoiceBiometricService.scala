package services.voice

import models.{EnrollmentBatch, Egraph}


trait VoiceBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean]
  def verify(audio: Array[Byte], egraph: Egraph): Either[VoiceBiometricsError, VoiceVerificationResult]
}

// TODO: Write an integration test against VBG service to ensure our code works.
/**
 * Voice biometrics implementation that connects to dev account at VBG's Free Speech engine.
 */
class VBGDevFSVoiceBiometricService extends VoiceBiometricService {
  private val vbg = VBGDevFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    vbg.enroll(enrollmentBatch)
  }

  def verify(audio: Array[Byte], egraph: Egraph): Either[VoiceBiometricsError, VoiceVerificationResult] = {
    vbg.verify(audio, egraph)
  }
}

class YesMaamVoiceBiometricService extends VoiceBiometricService {

  def enroll(enrollmentBatch: EnrollmentBatch) = Right(true)

  def verify(audio: Array[Byte], egraph: Egraph) = {
    Right(new VoiceVerificationResult {
      val score: Long = 100
      val success = true
    })
  }
}