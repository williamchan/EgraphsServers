package services.video

import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler._
import java.io._
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

object EgraphVideoEncoder {

  val canvasWidth = 600
  val canvasHeight = 380

  /**
   * Generates an audio-less mp4 using the Xuggle library. See documentation at:
   * http://wiki.xuggle.com/MediaTool_Introduction#How_To_Take_Snapshots_Of_Your_Desktop
   *
   * For guidance on suitable frame-rates, see:
   * http://productforums.google.com/forum/#!searchin/youtube/video$20length$20incorrect/youtube/YCKdtgFw5Zk/s-c7B8R9rXIJ
   *
   * @param targetFilePath file path in which to store generated mp4
   * @param egraphImageFile file containing egraph still image
   * @param recipientName recipient name
   * @param celebrityName celebrity name
   * @param audioDuration duration of egraph audio, in milliseconds
   */
  def generateMp4SansAudio(targetFilePath: String,
                           egraphImageFile: File,
                           recipientName: String,
                           celebrityName: String,
                           audioDuration: Int) {
    val egraphImg = ImageIO.read(egraphImageFile)
    val writer = ToolFactory.makeWriter(targetFilePath)
    writer.addVideoStream(/*inputIndex*/ 0, /*streamId*/ 0, /*codecId*/ ICodec.ID.CODEC_ID_MPEG4, /*width*/ canvasWidth, /*height*/ canvasHeight)

    /* Show egraph image for duration of egraph audio at 10fps. 8fps minimum was recommended by Google. */
    for (t <- 0 to audioDuration by 100) {
      writer.encodeVideo(/*streamIndex*/ 0, /*image*/ egraphImg, /*timeStamp*/ t, /*timeUnit*/ TimeUnit.MILLISECONDS)
    }

    writer.close()
  }

  /**
   * Combines video with audio using the mp4parser library. See documentation at:
   * http://code.google.com/p/mp4parser/wiki/Examples
   *
   * @param mp4File mp4 file containing (audio-less) video
   * @param aacFile aac file containing audio
   * @param targetFile file in which to store combined audio and mp4 video
   */
  def muxVideoWithAudio(mp4File: File,
                        aacFile: File,
                        targetFile: File) {
    val audioInputStream = new FileInputStream(aacFile)
    val fos = new FileOutputStream(targetFile)
    try {
      val video = MovieCreator.build(new FileInputStream(mp4File).getChannel)
      val aacTrack = new AACTrackImpl(/*inputStream*/ audioInputStream)
      video.addTrack(aacTrack)
      val out = new DefaultMp4Builder().build(video)
      out.getBox(fos.getChannel)
    } finally {
      fos.close()
      audioInputStream.close()
    }
  }
}
