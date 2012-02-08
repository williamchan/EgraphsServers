package services.signature

import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub
import play.Play

trait SignatureBiometricService {
  def verify(signatureJson: String, profileId: String): Either[SignatureBiometricsError, SignatureVerificationMetadata]
}

/**
 * SignatureBiometricService implementation that hits the configured Xyzmo signature biometric server.
 */
class XyzmoSignatureBiometricService extends SignatureBiometricService {
  def verify(signatureJson: String, profileId: String): Either[SignatureBiometricsError, SignatureVerificationMetadata] = {
    val sdc = XyzmoBiometricServices.getSignatureDataContainerFromJSON(signatureJson).getGetSignatureDataContainerFromJSONResult
    val verifyUserResponse = XyzmoBiometricServices.verifyUser(userId = profileId.toString, sdc)
    val verifyResult = verifyUserResponse.getOkInfo.getVerifyResult

    Right(SignatureVerificationMetadata(
      success=(verifyResult == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch),
      score=verifyUserResponse.getOkInfo.getScore
    ))
  }
}

/**
 * Implementation of [[services.signature.SignatureBiometricService]] that always
 * returns that the sample passed.
 */
class NiceSignatureBiometricService extends SignatureBiometricService {
  override def verify(signatureJson: String, profileId: String) = {
    play.Logger.info("Making a nice signature")
    Right(SignatureVerificationMetadata(
      success=true,
      score=100
    ))
  }
}

class SignatureBiometricsError extends RuntimeException

case class SignatureVerificationMetadata(success: Boolean, score: Int)