package services.signature

import models.{EnrollmentBatch, Egraph}
import models.xyzmo.XyzmoVerifyUser


trait SignatureBiometricService {
  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean]
  def verify(egraph: Egraph): Either[SignatureBiometricsError, XyzmoVerifyUser]
}

/**
 * SignatureBiometricService implementation that hits our live Xyzmo server for real Celebrities.
 *
 * IMPORTANT! -- Do not write tests for XyzmoProdBiometricServices or XyzmoBetaBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on Xyzmo.
 * Instead, XyzmoTestBiometricServices exists for automated tests of live Xyzmo server.
 */
class XyzmoProdSignatureBiometricService extends SignatureBiometricService {
  private val prod = XyzmoProdBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    prod.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[SignatureBiometricsError, XyzmoVerifyUser] = {
    prod.verify(egraph = egraph)
  }
}

/**
 * SignatureBiometricService implementation that hits our live Xyzmo server for test Celebrities.
 *
 * IMPORTANT! -- Do not write tests for XyzmoProdBiometricServices or XyzmoBetaBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on Xyzmo.
 * Instead, XyzmoTestBiometricServices exists for automated tests of live Xyzmo server.
 */
class XyzmoBetaSignatureBiometricService extends SignatureBiometricService {
  private val beta = XyzmoBetaBiometricServices

  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    beta.enroll(enrollmentBatch)
  }

  def verify(egraph: Egraph): Either[SignatureBiometricsError, XyzmoVerifyUser] = {
    beta.verify(egraph = egraph)
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
