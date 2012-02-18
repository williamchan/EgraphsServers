package services.signature


trait SignatureBiometricService {
  def verify(signatureJson: String, profileId: String): Either[SignatureBiometricsError, SignatureVerificationMetadata]
}

/**
 * SignatureBiometricService implementation that hits the configured Xyzmo signature biometric server.
 */
class XyzmoSignatureBiometricService extends SignatureBiometricService {
  def verify(signatureJson: String, profileId: String): Either[SignatureBiometricsError, SignatureVerificationMetadata] = {
    val sdc = TestXyzmoBiometricServices.getSignatureDataContainerFromJSON(signatureJson)
    val verifyUserResponse = TestXyzmoBiometricServices.verifyUser(userId = profileId.toString, sdc)

    Right(SignatureVerificationMetadata(
      success = (verifyUserResponse._2),
      score = verifyUserResponse._3
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
      score = 100
    ))
  }
}

class SignatureBiometricsError extends RuntimeException

case class SignatureVerificationMetadata(success: Boolean, score: Int)