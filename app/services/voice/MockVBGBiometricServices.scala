package services.voice

import java.net.URL

object MockVBGBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = null
  override protected val _myClientName: String = null
  override protected val _myClientKey: String = null

  override def sendStartEnrollmentRequest(userId: String, rebuildTemplate: Boolean): VBGRequest = {
    null
  }

  override def sendAudioCheckRequest(transactionId: String, blobLocation: String): VBGRequest = {
    null
  }

  override def sendEnrollUserRequest(transactionId: String): VBGRequest = {
    null
  }

  override def sendFinishEnrollTransactionRequest(transactionId: String, successValue: String): VBGRequest = {
    null
  }

  override def sendFinishVerifyTransactionRequest(transactionId: String, successValue: String, score: String): VBGRequest = {
    null
  }

  override def sendStartVerificationRequest(userId: String): VBGRequest = {
    null
  }

  override def sendVerifySampleRequest(transactionId: String, wavBinary: Array[Byte]): VBGRequest = {
    null
  }
}
