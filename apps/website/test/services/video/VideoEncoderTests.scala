package services.video

import utils.{TestHelpers, EgraphsUnitTest}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import javax.sound.sampled.{AudioInputStream, AudioFormat, AudioSystem}
import play.api.Play._
import services.audio.AudioConverter
import java.io._
import services.Utils

@RunWith(classOf[JUnitRunner])
class VideoEncoderTests extends EgraphsUnitTest {

  private val filenameAudioAac = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro.aac"

  "generate" should "generate video file" in new EgraphsTestApplication {
//    val mp3 = TestHelpers.fileAsBytes("test/4sec.mp3")
//    val aac = AudioConverter.convertToAAC(sourceAudio = mp3, tempFilesId = "VideoEncoderTests")
//    Utils.saveToFile(aac, new File("/Users/willchan83/github/eGraphsServers/apps/website/test/4sec.aac"))

//    AudioConverter.getDurationOfWav(current.getFile("test/pedro.wav")) should be(29)

    VideoEncoder.generateFinalAudio()
    VideoEncoder.generateMp4_no_audio_xuggle()
    VideoEncoder.muxVideoWithAAC_mp4parser()
  }

}
