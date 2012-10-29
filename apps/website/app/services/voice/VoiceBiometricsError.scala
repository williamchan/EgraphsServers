package services.voice

import models.vbg.VBGBase

abstract class VoiceBiometricsError(message: String) extends RuntimeException(message) {
  this: RuntimeException =>
  def code: String
}

class UnknownVoiceBiometricsError(val code: String, vbgBase: VBGBase)
  extends VoiceBiometricsError(
    "Voice biometrics error code = " + code + " while processing: " + vbgBase.toString + ".")

class KnownVoiceBiometricsError(val codeObj: VoiceBiometricsCode.EnumVal, vbgBase: VBGBase)
  extends VoiceBiometricsError(
    "Voice biometrics error of type \"" + codeObj.desc + "\" (" + codeObj.name + ")" +
      "while processing: " + vbgBase.toString + ".") {
  def code = codeObj.name
}

object VoiceBiometricsError {
  def apply(code: VoiceBiometricsCode.EnumVal, vbgBase: VBGBase): VoiceBiometricsError = {
    new KnownVoiceBiometricsError(code, vbgBase)
  }

  def apply(code: String, request: VBGBase): VoiceBiometricsError = {
    VoiceBiometricsCode.byCodeString.get(code) match {
      case Some(knownVbgCode) =>
        VoiceBiometricsError(knownVbgCode, request)

      case None =>
        new UnknownVoiceBiometricsError(code, request)
    }
  }
}