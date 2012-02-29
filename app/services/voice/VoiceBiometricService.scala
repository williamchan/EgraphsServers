package services.voice

trait VoiceBiometricService {
  def verify(audio: Array[Byte], userId: String): Either[VoiceBiometricsError, VoiceVerificationResult]
}

// TODO: Write an integration test against VBG service to ensure our code works.
/**
 * VBG implementation of voice biometric services.
 */
class VBGVoiceBiometricService extends VoiceBiometricService {
  val vbg = VBGDevFreeSpeechBiometricServices

  def verify(audio: Array[Byte], userId: String): Either[VoiceBiometricsError, VoiceVerificationResult] = {
    // Begin the verification transaction
    vbg.requestStartVerification(userId).right.flatMap { startVerificationResponse =>
      val transactionId = startVerificationResponse.transactionId

      // Upload the sample and get the verification results
      var success = false
      var score: Long = 0
      
      try {
        val errorOrVerification = vbg.requestVerifySample(transactionId, audio)
        for (verification <- errorOrVerification.right) {
          success = verification.success
          score = verification.score
        }

        errorOrVerification
      }
      finally {
        // Close out the transaction regardless of outcome
        vbg.requestFinishVerifyTransaction(transactionId, success, score)
      }
    }
  }
}

class YesMaamVoiceBiometricService extends VoiceBiometricService {
  def verify(audio: Array[Byte], userId: String) = {
    Right(new VoiceVerificationResult {
      val score: Long = 100
      val success = true
    })
  }
}