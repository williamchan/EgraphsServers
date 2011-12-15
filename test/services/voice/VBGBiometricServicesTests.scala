package services.voice

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers


class VBGBiometricServicesTests extends UnitFlatSpec with ShouldMatchers {

  "getDownSampledBase64" should "convert" in {
    val voiceSampleBase64 = VBGBiometricServices.getVoiceSampleBase64Encoded("test/files/44khz.wav")
    val wavBinary: Array[Byte] = VBGBiometricServices.getVoiceSampleBinary("test/files/44khz.wav")
    val voiceSampleBase64_downSampled: String = VBGBiometricServices.getDownSampledBase64(wavBinary)
  }

}