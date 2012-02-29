package services.signature


trait SignatureBiometricService {
  def verify(signatureJson: String, profileId: String): Either[SignatureBiometricsError, SignatureVerificationMetadata]
}

/**
 * SignatureBiometricService implementation that hits the configured Xyzmo signature biometric server.
 */
class XyzmoSignatureBiometricService extends SignatureBiometricService {
  def verify(signatureJson: String, profileId: String): Either[SignatureBiometricsError, SignatureVerificationMetadata] = {
    val sdc = XyzmoBiometricServices.getSignatureDataContainerFromJSON(signatureJson)
    val verifyUser = XyzmoBiometricServices.verifyUser(userId = profileId.toString, sdc)

    Right(SignatureVerificationMetadata(
      success = verifyUser.isMatch.getOrElse(false),
      score = verifyUser.score
    ))
  }
}

/**
 * Implementation of [[services.signature.SignatureBiometricService]] that always
 * returns that the sample passed.
 */
class NiceSignatureBiometricService extends SignatureBiometricService {
  override def verify(signatureJson: String, profileId: String) = {
    Right(SignatureVerificationMetadata(
      success = true,
      score = Some(100)
    ))
  }
}

class SignatureBiometricsError extends RuntimeException

case class SignatureVerificationMetadata(success: Boolean, score: Option[Int])