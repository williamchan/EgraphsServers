package services.voice

import models.{EnrollmentBatch, Egraph}
import models.vbg.VBGVerifySample


trait VoiceBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean]
  def verify(audio: Array[Byte], egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample]
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

  def verify(audio: Array[Byte], egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    vbg.verify(audio, egraph)
  }
}

class YesMaamVoiceBiometricService extends VoiceBiometricService {

  def enroll(enrollmentBatch: EnrollmentBatch) = Right(true)

  def verify(audio: Array[Byte], egraph: Egraph) = {
    val vbgVerifySample = new VBGVerifySample(
      egraphId = egraph.id,
      errorCode = VoiceBiometricsCode.Success.name,
      score = Some(100),
      success = Some(true)
    ).save()
    Right(vbgVerifySample)
  }
}