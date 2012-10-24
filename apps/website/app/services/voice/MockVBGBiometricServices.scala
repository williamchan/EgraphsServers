package services.voice

import java.net.URL
import models._
import vbg._

object MockVBGBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = null
  override protected val _myClientName: String = null
  override protected val _myClientKey: String = null
  override protected val _userIdPrefix: String = "mock"

  override protected[voice] def sendAudioCheckRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long, wavBinary: Array[Byte]): VBGAudioCheck = {
    new VBGAudioCheck(enrollmentBatchId = enrollmentBatch.id, vbgTransactionId = transactionId, errorCode = VoiceBiometricsCode.Success.name)
  }

  override protected[voice] def sendEnrollUserRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long): VBGEnrollUser = {
    new VBGEnrollUser(enrollmentBatchId = enrollmentBatch.id, vbgTransactionId = transactionId, errorCode = VoiceBiometricsCode.Success.name, success = Some(true))
  }

  override protected[voice] def sendFinishEnrollTransactionRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long, successValue: Boolean): VBGFinishEnrollTransaction = {
    new VBGFinishEnrollTransaction(enrollmentBatchId = enrollmentBatch.id, vbgTransactionId = transactionId, errorCode = VoiceBiometricsCode.Success.name)
  }

  override protected[voice] def sendFinishVerifyTransactionRequest(egraph: Egraph, transactionId: Long, successValue: Boolean, score: Long): VBGFinishVerifyTransaction = {
    new VBGFinishVerifyTransaction(egraphId = egraph.id, vbgTransactionId = transactionId, errorCode = VoiceBiometricsCode.Success.name)
  }

  override protected[voice] def sendStartEnrollmentRequest(enrollmentBatch: EnrollmentBatch, rebuildTemplate: Boolean): VBGStartEnrollment = {
    new VBGStartEnrollment(enrollmentBatchId = enrollmentBatch.id, errorCode = VoiceBiometricsCode.Success.name, vbgTransactionId = Some(0))
  }

  override protected[voice] def sendStartVerificationRequest(egraph: Egraph): VBGStartVerification = {
    new VBGStartVerification(egraphId = egraph.id, errorCode = VoiceBiometricsCode.Success.name, vbgTransactionId = Some(0))
  }

  override protected[voice] def sendVerifySampleRequest(egraph: Egraph, transactionId: Long): VBGVerifySample = {
    new VBGVerifySample(egraphId = egraph.id, vbgTransactionId = transactionId, errorCode = VoiceBiometricsCode.Success.name)
  }
}
