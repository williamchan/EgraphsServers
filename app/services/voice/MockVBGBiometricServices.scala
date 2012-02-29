package services.voice

import java.net.URL

object MockVBGBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = null
  override protected val _myClientName: String = null
  override protected val _myClientKey: String = null

  override def sendAudioCheckRequest(transactionId: Long, blobLocation: String) = null

  override def sendEnrollUserRequest(transactionId: Long) = null

  override def sendFinishEnrollTransactionRequest(transactionId: Long, successValue: Boolean) = null

  override def sendFinishVerifyTransactionRequest(transactionId: Long, successValue: Boolean, score: Long) = null

  override def sendStartEnrollmentRequest(userId: String, rebuildTemplate: Boolean) = null

  override def sendStartVerificationRequest(userId: String) = null

  override def sendVerifySampleRequest(transactionId: Long, wavBinary: Array[Byte]) = null
}
