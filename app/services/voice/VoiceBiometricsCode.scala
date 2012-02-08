package services.voice

import services.Utils.Enum

/**
 * Error codes from the VBG Voice Biometrics API. See `Dropbox/Development/voice biometrics`
 * for API documentation.
 */
object VoiceBiometricsCode extends Enum {
  sealed trait EnumVal extends Value { def name: String }

  val Success = new EnumVal { final val name = "0" }
  val InvalidSampleSequenceNumber = new EnumVal{ val name = "51300" }
  val NoSpeechDetectedInSample = new EnumVal{ val name = "90100"}
  val SpeechSampleAmplitudeTooLow = new EnumVal{ val name = "90200"}
  val SpeechSampleContainsTooMuchBackgroundNoise = new EnumVal{ val name = "90400"}
  val UserSpokeTooQuicklyOrSampleIncomplete = new EnumVal{ val name = "90500"}
  val GeneralProblemWithRecording = new EnumVal{ val name = "90700"}
  val SpeechSampleTooLong = new EnumVal{ val name = "91100"}
  val SpeechSampleAmplitudeTooHigh = new EnumVal{ val name = "92200"}
  val UnableToCreateVoicePrint = new EnumVal{ val name = "93300"}
  val VoiceSystemError = new EnumVal{ val name = "99100"}

  val byCodeString: Map[String, EnumVal] = values.map(enumVal => (enumVal.name, enumVal)).toMap
}






