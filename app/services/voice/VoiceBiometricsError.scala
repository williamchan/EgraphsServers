package services.voice

abstract class VoiceBiometricsError(message: String) extends RuntimeException(message) { this: RuntimeException =>
  def code: String
}

class UnknownVoiceBiometricsError(val code: String, request: VBGRequest)
  extends VoiceBiometricsError(
    "Voice biometrics error code = " + code + " while processing request type " + request.getResponseType + ".")

class KnownVoiceBiometricsError(val codeObj: VoiceBiometricsCode.EnumVal, request: VBGRequest)
  extends VoiceBiometricsError(
    "Voice biometrics error while processing request type " + request.getResponseType + ": " + codeObj.desc +
    " (" + codeObj.name + ")"
  )
{
  def code = codeObj.name
}

object VoiceBiometricsError {
  def apply (code: VoiceBiometricsCode.EnumVal, request: VBGRequest):VoiceBiometricsError = {
    new KnownVoiceBiometricsError(code, request)
  }
  
  def apply (code: String, request: VBGRequest): VoiceBiometricsError = {
    VoiceBiometricsCode.byCodeString.get(code) match {
      case Some(knownVbgCode) =>
        VoiceBiometricsError(knownVbgCode, request)

      case None =>
        new UnknownVoiceBiometricsError(code, request)
    }
  }
}