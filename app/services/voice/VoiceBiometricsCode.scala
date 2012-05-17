package services.voice

import services.Utils.Enum

/**
 * Error codes from the VBG Voice Biometrics API. See `Dropbox/Development/voice biometrics`
 * for API documentation.
 */
object VoiceBiometricsCode extends Enum {
  sealed trait EnumVal extends Value { def name: String; def desc: String }

  val Success = new EnumVal {
    final val name = "0"
    final val desc = "Success"
  }

  val UserIdAlreadyExists = new EnumVal{
    final val name = "50500";
    val desc = "Parameter value for <userid> already exists"
  }

  val InvalidSampleSequenceNumber = new EnumVal{
    final val name = "51300";
    val desc = "Invalid sample sequence number"
  }

  val NoSpeechDetectedInSample = new EnumVal{
    final val name = "90100";
    val desc = "No speech detected in sample"
  }

  val SpeechSampleAmplitudeTooLow = new EnumVal{
    final val name = "90200";
    val desc = "Speech sample amplitude too low"
  }

  val SpeechSampleContainsTooMuchBackgroundNoise = new EnumVal{
    final val name = "90400";
    val desc = "Speech sample contains too much background noise"
  }
  val UserSpokeTooQuicklyOrSampleIncomplete = new EnumVal{
    final val name = "90500";
    val desc = "User spoke too quickly or sample was incomplete"
  }
  val GeneralProblemWithRecording = new EnumVal{
    final val name = "90700";
    val desc = "General problem with recording."
  }
  val TooLittleSpeechReceived = new EnumVal{
    final val name = "90800";
    val desc = "Too little speech received."
  }
  val SpeechSampleTooLong = new EnumVal{
    final val name = "91100";
    val desc = "Speech sample too long"
  }
  val SpeechSampleAmplitudeTooHigh = new EnumVal{
    final val name = "92200";
    val desc = "Speech sample amplitude too high"
  }
  val UnableToCreateVoicePrint = new EnumVal{
    final val name = "93300";
    val desc = "Unable to create voice print"
  }
  val VoiceSystemError = new EnumVal{
    final val name = "99100";
    val desc = "Voice system error"
  }

  val byCodeString: Map[String, EnumVal] = values.map(enumVal => (enumVal.name, enumVal)).toMap
}






