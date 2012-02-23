package services.voice

import play.Play
import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import services.blobs.Blobs
import utils.TestConstants
import play.libs.Codec
import javax.sound.sampled.{AudioInputStream, AudioFileFormat, AudioSystem}
import java.io.ByteArrayOutputStream


class VBGBiometricServicesTests extends UnitFlatSpec with ShouldMatchers {

  "convertWavTo8kHzBase64" should "convert to 8khz encoded in base64" in {
    val wavBinary_44kHz: Array[Byte] = getVoiceSampleBinary("test/files/44khz.wav")
    val wav_8kHz_base64: String = MockVBGBiometricServices.convertWavTo8kHzBase64(wavBinary_44kHz)
    wav_8kHz_base64 should be(TestConstants.voiceStr_8khz())
  }

  "stitchWAVs" should "stitch multiple WAVs together" in {
    val filename = "test/files/44khz.wav"
    val targetFile = "test/files/stitched_3x.wav"
    val resultFile = Play.getFile(targetFile)

    val appendedFiles: Option[AudioInputStream] = MockVBGBiometricServices.stitchWAVs(List(getVoiceSampleBinary(filename), getVoiceSampleBinary(filename), getVoiceSampleBinary(filename)))
    AudioSystem.write(appendedFiles.get, AudioFileFormat.Type.WAVE, resultFile)
    Play.getFile(targetFile).length() should be(921166)
  }

  "stitchWAVs" should "handle base cases" in {
    MockVBGBiometricServices.stitchWAVs(List()) should be(None)

    val filename = "test/files/44khz.wav"
    val result: AudioInputStream = MockVBGBiometricServices.stitchWAVs(List(getVoiceSampleBinary(filename))).get
    val audioISFromFile: AudioInputStream = AudioSystem.getAudioInputStream(Play.getFile(filename))
    Codec.encodeBASE64(convertAudioInputStreamToByteArray(result)) should be(Codec.encodeBASE64(convertAudioInputStreamToByteArray(audioISFromFile)))
  }

  private def convertAudioInputStreamToByteArray(audioInputStream: AudioInputStream): Array[Byte] = {
    val bas: ByteArrayOutputStream = new ByteArrayOutputStream()
    AudioSystem.write(audioInputStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, bas)
    bas.toByteArray
  }

  private def getVoiceSampleBinary(filename: String): Array[Byte] = {
    val file = Play.getFile(filename)
    Blobs.Conversions.fileToByteArray(file)
  }

}