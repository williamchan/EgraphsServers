package services.voice

import libs.Blobs
import play.libs.Codec
import play.Play
import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers


class VBGBiometricServicesTests extends UnitFlatSpec with ShouldMatchers {

  // This unfinished test will become meaningful once VBGBiometricServices.getDownSampledBase64 actually does some down-sampling
  "getDownSampledBase64" should "convert" in {
    val voiceSampleBase64 = Codec.encodeBASE64(getVoiceSampleBinary("test/files/44khz.wav"))
    val wavBinary: Array[Byte] = getVoiceSampleBinary("test/files/44khz.wav")
    val voiceSampleBase64_downSampled: String = VBGBiometricServices.getDownSampledBase64(wavBinary)
  }

  private def getVoiceSampleBinary(filename : String): Array[Byte] = {
    val file = Play.getFile(filename)
    Blobs.Conversions.fileToByteArray(file)
  }

}