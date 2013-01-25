package services.video

import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler._
import javax.imageio.ImageIO
import java.io._
import java.util.concurrent.TimeUnit
import java.awt.image.BufferedImage
import java.awt.{Color, Font, RenderingHints}
import services.blobs.Blobs

object VideoEncoder {

  val canvasWidth = 592
  val canvasHeight = 374

  def generateFinalAudio(sourceAacFile: File,
                         targetFile: File) {
    val fos = new FileOutputStream(targetFile)
    // TODO(egraph-exploration): uncomment when we can stitch multiple images into an mp4
    //    fos.write(IOUtils.toByteArray(current.resourceAsStream("audio/4sec.aac").get))
    fos.write(Blobs.Conversions.fileToByteArray(sourceAacFile))
    fos.flush()
    fos.close()
  }

  def generateMp4_no_audio_xuggle(targetFileName: String,
                                  egraphImageFile: File,
                                  recipientName: String,
                                  celebrityName: String,
                                  audioDuration: Int) {
    // TODO(egraph-exploration): uncomment when we can stitch multiple images into an mp4
    //    val preambleImg = generatePreamble(recipientName, celebrityName)

    val egraphImg = getEgraphImg(egraphImageFile)

    val writer = ToolFactory.makeWriter(targetFileName)
    writer.addVideoStream(/*inputIndex*/ 0, /*streamId*/ 0, /*codecId*/ ICodec.ID.CODEC_ID_MPEG4, /*width*/ egraphImg.getWidth, /*height*/ egraphImg.getHeight)

    val startTime = System.nanoTime()

    // TODO(egraph-exploration): uncomment when we can stitch multiple images into an mp4
    // show preamble for 4 seconds
    //    for (i <- 0 until 4) {
    //      writer.encodeVideo(/*streamIndex*/ 0, /*image*/ preambleImg, /*timeStamp*/ System.nanoTime() - startTime, /*timeUnit*/ TimeUnit.NANOSECONDS)
    //      Thread.sleep(1000)
    //    }

    // show egraph image for duration of egraph audio
    for (i <- 0 until audioDuration) {
      writer.encodeVideo(/*streamIndex*/ 0, /*image*/ egraphImg, /*timeStamp*/ System.nanoTime() - startTime, /*timeUnit*/ TimeUnit.NANOSECONDS)
      Thread.sleep(1000)
    }

    writer.close()
  }

  private def getEgraphImg(egraphImageFile: File): BufferedImage = {
    ImageIO.read(egraphImageFile)
  }

  def muxVideoWithAAC_mp4parser(videoFile: File,
                                audioFile: File,
                                targetFile: File) {
    val audioInputStream = new FileInputStream(audioFile)
    val fos = new FileOutputStream(targetFile)

    val video = MovieCreator.build(new FileInputStream(videoFile).getChannel)
    val aacTrack = new AACTrackImpl(/*inputStream*/ audioInputStream)
    video.addTrack(aacTrack)
    val out = new DefaultMp4Builder().build(video)
    out.getBox(fos.getChannel)
    fos.close()
  }

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
}
