package services.video

import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler._
import javax.imageio.ImageIO
import java.io._
import java.util.concurrent.TimeUnit
import org.apache.commons.io.IOUtils
import play.api.Play.current
import services.blobs.Blobs

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

    /**
     * TODO(egraph-exploration): uncomment when we can stitch multiple images into an mp4, and adjust t on egraphImg screens
     * val preambleImg = generatePreamble(recipientName, celebrityName)
     *    // show preamble for 4 seconds
     *    for (t <- 0 to 4000 by 100) {
     *      writer.encodeVideo(/*streamIndex*/ 0, /*image*/ preambleImg, /*timeStamp*/ t, /*timeUnit*/ TimeUnit.MILLISECONDS)
     *    }
     */

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
   * @param videoFile file containing (audio-less) video
   * @param audioFile file containing audio
   * @param targetFile file in which to store combined audio and mp4 video
   */
  def muxVideoWithAudio(videoFile: File,
                        audioFile: File,
                        targetFile: File) {
    val audioInputStream = new FileInputStream(audioFile)
    val fos = new FileOutputStream(targetFile)
    try {
      val video = MovieCreator.build(new FileInputStream(videoFile).getChannel)
      val aacTrack = new AACTrackImpl(/*inputStream*/ audioInputStream)
      video.addTrack(aacTrack)
      val out = new DefaultMp4Builder().build(video)
      out.getBox(fos.getChannel)
    } finally {
      fos.close()
      audioInputStream.close()
    }
  }

  /**
   * Generates aac audio prefixed with four seconds of silence. Meant to be used with video that contains a preamble.
   *
   * @param sourceAacFile aac audio file containing the egraph audio
   * @param targetFile file in which to store the resulting audio
   */
  def generateFinalAudio(sourceAacFile: File,
                         targetFile: File) {
    val fos = new FileOutputStream(targetFile)
    try {
      fos.write(IOUtils.toByteArray(current.resourceAsStream("audio/4sec.aac").get))
      fos.write(Blobs.Conversions.fileToByteArray(sourceAacFile))
      fos.flush()
    } finally {
      fos.close()
    }
  }

  /*
  /**
   * @param recipientName recipient name
   * @param celebrityName celebrity name
   * @return preamble image introducing the egraph
   */
  private def generatePreamble(recipientName: String, celebrityName: String): BufferedImage = {
    val canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_3BYTE_BGR)
    val g2 = canvas.createGraphics
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2.setColor(Color.WHITE)
    g2.fillRect(0, 0, canvasWidth, canvasHeight)

    val font = new Font("Century Gothic", Font.PLAIN, 30)
    g2.setFont(font)
    g2.setColor(Color.BLACK)

    var frc = g2.getFontRenderContext
    var fontRec = font.getStringBounds("An egraph for " + recipientName, frc)
    var x = ((canvasWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("An egraph for " + recipientName, x, 150)

    frc = g2.getFontRenderContext
    fontRec = font.getStringBounds("from " + celebrityName, frc)
    x = ((canvasWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("from " + celebrityName, x, 225)
    g2.dispose()

    canvas
  }
  */
}
