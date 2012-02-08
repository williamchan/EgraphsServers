package services.voice

import services.Utils.Enum

trait VoiceVerificationResult {
  def success: Boolean
  def score: Int
}





