package services.signature

import models.{EnrollmentBatch, Egraph}
import models.xyzmo.XyzmoVerifyUser


trait SignatureBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean]
  def verify(egraph: Egraph): Either[SignatureBiometricsError, XyzmoVerifyUser]
}

/**
 * SignatureBiometricService implementation that hits the configured Xyzmo signature biometric server.
 */
class XyzmoSignatureBiometricService extends SignatureBiometricService {
  private val xyzmo = XyzmoBiometricServices
  
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    xyzmo.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[SignatureBiometricsError, XyzmoVerifyUser] = {
    xyzmo.verify(egraph = egraph)
  }
}

/**
 * SignatureBiometricService implementation that hits testlab.xyzmo.com.
 */
class TestlabXyzmoSignatureBiometricService extends SignatureBiometricService {
  private val testlab = TestXyzmoBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    testlab.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[SignatureBiometricsError, XyzmoVerifyUser] = {
    testlab.verify(egraph = egraph)
  }
}

/**
 * Implementation of [[services.signature.SignatureBiometricService]] that always
 * returns that the sample passed.
 */
class YesMaamSignatureBiometricService extends SignatureBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    Right(true)
  }

  override def verify(egraph: Egraph) = {
    val xyzmoVerifyUser = new XyzmoVerifyUser(
      egraphId = egraph.id,
      baseResult = "ok",
      isMatch = Some(true),
      score = Some(100)
    ).save()
    Right(xyzmoVerifyUser)
  }
}

class SignatureBiometricsError extends RuntimeException
