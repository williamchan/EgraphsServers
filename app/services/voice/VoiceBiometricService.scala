package services.voice

import models.{EnrollmentBatch, Egraph}
import models.vbg.VBGVerifySample


trait VoiceBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean]
  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample]
}

/**
* Voice biometrics implementation that connects to prod account at VBG's Free Speech engine.
*/
class VBGProdFSVoiceBiometricService extends VoiceBiometricService {
  private val vbg = VBGProdFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    vbg.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    vbg.verify(egraph)
  }
}

/**
 * Voice biometrics implementation that connects to dev account at VBG's Free Speech engine.
 */
class VBGDevFSVoiceBiometricService extends VoiceBiometricService {
  private val vbg = VBGDevFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    vbg.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    vbg.verify(egraph)
  }
}

class YesMaamVoiceBiometricService extends VoiceBiometricService {

  def enroll(enrollmentBatch: EnrollmentBatch) = Right(true)

  def verify(egraph: Egraph) = {
    val vbgVerifySample = new VBGVerifySample(
      egraphId = egraph.id,
      errorCode = VoiceBiometricsCode.Success.name,
      score = Some(100),
      success = Some(true)
    ).save()
    Right(vbgVerifySample)
  }
}