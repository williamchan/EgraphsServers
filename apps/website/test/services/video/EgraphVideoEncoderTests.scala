package services.video

import utils.EgraphsUnitTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.TempFile
import services.blobs.Blobs

@RunWith(classOf[JUnitRunner])
class EgraphVideoEncoderTests extends EgraphsUnitTest {

  "generateMp4SansAudio" should "generate an mp4 without sound" in new EgraphsTestApplication {
    val file = TempFile.named("generateMp4SansAudio.mp4")
    EgraphVideoEncoder.generateMp4SansAudio(
      targetFilePath = file.getPath,
      egraphImageFile = resourceFile("egraph.jpg"),
      recipientName = "Jordan",
      celebrityName = "Sergio romo",
      audioDuration = 1000
    )
    Blobs.Conversions.fileToByteArray(file).length should be(88729)
  }

  "muxVideoWithAudio" should "combine mp4 video with aac audio into a new mp4 file" in new EgraphsTestApplication {
    val targetFile = TempFile.named("muxVideoWithAudio-final.mp4")
    EgraphVideoEncoder.muxVideoWithAudio(
      mp4File = resourceFile("muxVideoWithAudio.mp4"),
      aacFile = resourceFile("muxVideoWithAudio.aac"),
      targetFile = targetFile
    )
    Blobs.Conversions.fileToByteArray(targetFile).length should be(80210)
  }
}
