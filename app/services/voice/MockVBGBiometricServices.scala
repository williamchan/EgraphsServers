package services.voice

import java.net.URL
import models._

object MockVBGBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = null
  override protected val _myClientName: String = null
  override protected val _myClientKey: String = null

  override protected def sendAudioCheckRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long, wavBinary: Array[Byte]) = null

  override protected def sendEnrollUserRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long) = null

  override protected def sendFinishEnrollTransactionRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long, successValue: Boolean) = null

  override protected def sendFinishVerifyTransactionRequest(egraph: Egraph, transactionId: Long, successValue: Boolean, score: Long) = null

  override protected def sendStartEnrollmentRequest(enrollmentBatch: EnrollmentBatch, rebuildTemplate: Boolean) = null

  override protected def sendStartVerificationRequest(egraph: Egraph) = null

  override protected def sendVerifySampleRequest(egraph: Egraph, transactionId: Long, wavBinary: Array[Byte]) = null
}
