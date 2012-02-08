package services.voice

import services.Utils.Enum
import services.Utils

class VoiceBiometricsError(val code: String, request: VBGRequest) extends RuntimeException("Voice biometrics error code = " + code + "while processing request type" + request.getResponseType)

object VoiceBiometricsError {
  def apply (code: VoiceBiometricsCode.EnumVal, request: VBGRequest):VoiceBiometricsError = {
    new VoiceBiometricsError(code.name, request)
  }
}