package services.signature

import models.{EnrollmentBatch, Egraph}


trait SignatureBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean]
  def verify(signatureJson: String, egraph: Egraph): Either[SignatureBiometricsError, SignatureVerificationMetadata]
}

/**
 * SignatureBiometricService implementation that hits the configured Xyzmo signature biometric server.
 */
class XyzmoSignatureBiometricService extends SignatureBiometricService {
  private val xyzmo = XyzmoBiometricServices
  
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    xyzmo.enroll(enrollmentBatch)
  }

  def verify(signatureJson: String, egraph: Egraph): Either[SignatureBiometricsError, SignatureVerificationMetadata] = {
    xyzmo.verify(egraph = egraph, signatureJson = signatureJson)
  }
}

/**
 * Implementation of [[services.signature.SignatureBiometricService]] that always
 * returns that the sample passed.
 */
class NiceSignatureBiometricService extends SignatureBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    Right(true)
  }

  override def verify(signatureJson: String, egraph: Egraph) = {
    Right(SignatureVerificationMetadata(
      success = true,
      score = Some(100)
    ))
  }
}

class SignatureBiometricsError extends RuntimeException

case class SignatureVerificationMetadata(success: Boolean, score: Option[Int])