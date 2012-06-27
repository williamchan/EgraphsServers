package services.audio

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import utils.TestHelpers
import services.TempFile

class AudioConverterTests extends UnitFlatSpec with ShouldMatchers {

  private val tempFilesId = "AudioConverterTests"

  "convertWavToMp3" should "return byte array of mp3 that is much smaller, and also leave no temp files" in {
    val wav = TestHelpers.fileAsBytes("test/files/44khz.wav")
    val mp3 = AudioConverter.convertWavToMp3(sourceAudio = wav, tempFilesId = tempFilesId)
    (mp3.length < (wav.length / 10)) should be(true)
    TempFile.named(tempFilesId + "/audio.wav").isFile should be(false)
    TempFile.named(tempFilesId + "/audio.mp3").isFile should be(false)
  }
}
