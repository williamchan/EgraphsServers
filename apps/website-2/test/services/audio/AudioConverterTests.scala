package services.audio

import utils.{EgraphsUnitTest, TestHelpers}
import services.TempFile

class AudioConverterTests extends EgraphsUnitTest {

  private val tempFilesId = "AudioConverterTests"

  "convertWavToMp3" should "return byte array of mp3 that is much smaller, and also leave no temp files" in new EgraphsTestApplication {
    val wav = TestHelpers.fileAsBytes("44khz.wav")
    val mp3 = AudioConverter.convertWavToMp3(sourceAudio = wav, tempFilesId = tempFilesId)
    mp3.length should be > (0)
    mp3.length should be < (wav.length / 10)
    TempFile.named(tempFilesId + "/audio.wav").isFile should be(false)
    TempFile.named(tempFilesId + "/audio.mp3").isFile should be(false)
  }
}
