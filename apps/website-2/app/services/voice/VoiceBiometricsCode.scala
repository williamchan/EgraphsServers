package services.voice

import services.Utils.Enum

/**
 * Error codes from the VBG Voice Biometrics API. See `Dropbox/Development/voice biometrics`
 * for API documentation.
 */
sealed abstract class VoiceBiometricsCode(val name: String, val desc: String)

object VoiceBiometricsCode extends Enum {
  sealed abstract class EnumVal(name: String, desc: String) extends VoiceBiometricsCode(name, desc) with Value

  val Success = new EnumVal("0", "Success") {}

  val UserIdAlreadyExists = new EnumVal("50500", "Parameter value for <userid> already exists") {} 

  val InvalidSampleSequenceNumber = new EnumVal("51300", "Invalid sample sequence number") {}

  val NoSpeechDetectedInSample = new EnumVal("90100", "No speech detected in sample") {}

  val SpeechSampleAmplitudeTooLow = new EnumVal(
    "90200",
    "Speech sample amplitude too low"
  ) {}

  val SpeechSampleContainsTooMuchBackgroundNoise = new EnumVal(
    "90400",
    "Speech sample contains too much background noise"
  ) {}
  
  val UserSpokeTooQuicklyOrSampleIncomplete = new EnumVal(
    "90500",
    "User spoke too quickly or sample was incomplete"
  ) {}
  
  val GeneralProblemWithRecording = new EnumVal(
    "90700",
    "General problem with recording."
  ) {}
  
  val TooLittleSpeechReceived = new EnumVal(
    "90800",
    "Too little speech received."
  ) {}
  
  val SpeechSampleTooLong = new EnumVal(
    "91100",
    "Speech sample too long"
  ) {}
  
  val SpeechSampleAmplitudeTooHigh = new EnumVal(
    "92200",
    "Speech sample amplitude too high"
  ) {}
  
  val UnableToCreateVoicePrint = new EnumVal(
    "93300",
    "Unable to create voice print"
  ) {}
  
  val VoiceSystemError = new EnumVal(
    "99100",
    "Voice system error"
  ) {}

  val byCodeString: Map[String, EnumVal] = values.map(enumVal => (enumVal.name, enumVal)).toMap
}






