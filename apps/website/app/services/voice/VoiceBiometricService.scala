package services.voice

import models.{EnrollmentBatch, Egraph}
import models.vbg.VBGVerifySample


trait VoiceBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean]
  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample]
}

/**
 * Voice biometrics implementation that connects to prod account at VBG's Free Speech engine for real Celebrities.
 *
 * IMPORTANT! -- Do not write tests for VBGProdFreeSpeechBiometricServices or VBGBetaFreeSpeechBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on VBG.
 * Instead, VBGTestFreeSpeechBiometricServices exists for automated tests of celebrity-fs-en account.
 */
class VBGProdFSVoiceBiometricService extends VoiceBiometricService {
  private val prod = VBGProdFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    prod.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    prod.verify(egraph)
  }
}

/**
 * Voice biometrics implementation that connects to prod account at VBG's Free Speech engine for test Celebrities.
 *
 * IMPORTANT! -- Do not write tests for VBGProdFreeSpeechBiometricServices or VBGBetaFreeSpeechBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on VBG.
 * Instead, VBGTestFreeSpeechBiometricServices exists for automated tests of celebrity-fs-en account.
 */
class VBGBetaFSVoiceBiometricService extends VoiceBiometricService {
  private val beta = VBGBetaFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    beta.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    beta.verify(egraph)
  }
}

/**
 * Voice biometrics implementation that connects to dev account at VBG's Free Speech engine.
 */
class VBGDevFSVoiceBiometricService extends VoiceBiometricService {
  private val dev = VBGDevFreeSpeechBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    dev.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    dev.verify(egraph)
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