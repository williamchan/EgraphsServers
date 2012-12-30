package services.video

import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.ICodec
import services.audio.AudioConverter

import javax.imageio.ImageIO
import java.io._
import java.util.concurrent.TimeUnit
import utils.TestHelpers
import java.awt.image.BufferedImage
import java.awt.{Color, Font, RenderingHints}

object VideoEncoder {

  private val filenameImage = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro.jpg"
  private val filenameAudioWav = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro.wav"
  private val filenameAudioMp3 = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro.mp3"
  private val filenameAudioAac = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro.aac"
  private val filenameAudioSilence = "/Users/willchan83/github/eGraphsServers/apps/website/resources/audio/4sec.aac"
  private val filenameAudioFinal = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro-final.aac"

  private val filenameVideoNoAudio = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro-no-audio.mp4"
  private val filenameVideoFinal = "/Users/willchan83/github/eGraphsServers/apps/website/test/pedro-final.mp4"

  def generatePreamble(): BufferedImage = {
    val canvasWidth = 595
    val canvasHeight = 376

    val canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_3BYTE_BGR)
    val g2 = canvas.createGraphics
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2.setColor(Color.WHITE)
    g2.fillRect(0, 0, canvasWidth, canvasHeight)

    val font = new Font("Century Gothic", Font.PLAIN, 30)
    g2.setFont(font)
    g2.setColor(Color.BLACK)

    // An egraph for Jordan
    var frc = g2.getFontRenderContext
    var fontRec = font.getStringBounds("An egraph for Jordan", frc)
    var x = ((canvasWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("An egraph for Jordan", x, 150)

    // from Sergio Romo
    frc = g2.getFontRenderContext
    fontRec = font.getStringBounds("from Sergio Romo", frc)
    x = ((canvasWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("from Sergio Romo", x, 225)

    g2.dispose()
    canvas
  }

  def generateFinalAudio() {
    val fos = new FileOutputStream(new File(filenameAudioFinal))
    fos.write(TestHelpers.fileAsBytes("test/4sec.aac"))
    fos.write(TestHelpers.fileAsBytes("test/pedro.aac"))
    fos.flush()
    fos.close()
  }

  def generateMp4_no_audio_xuggle() {
    val preambleImg = generatePreamble()
    val egraphImg = ImageIO.read(new File(filenameImage))

    val writer = ToolFactory.makeWriter(filenameVideoNoAudio)
    val audioDuration = AudioConverter.getDurationOfWav(new File(filenameAudioWav))

    writer.addVideoStream(/*inputIndex*/ 0, /*streamId*/ 0, /*codecId*/ ICodec.ID.CODEC_ID_MPEG4, /*width*/ egraphImg.getWidth, /*height*/ egraphImg.getHeight)
    val startTime = System.nanoTime()

    // show preamble for 4 seconds
    for (i <- 0 until 4) {
      writer.encodeVideo(/*streamIndex*/ 0, /*image*/ preambleImg, /*timeStamp*/ System.nanoTime() - startTime, /*timeUnit*/ TimeUnit.NANOSECONDS)
      Thread.sleep(1000)
    }

    // show egraph image for duration of egraph audio
    for (i <- 0 until audioDuration) {
      writer.encodeVideo(/*streamIndex*/ 0, /*image*/ egraphImg, /*timeStamp*/ System.nanoTime() - startTime, /*timeUnit*/ TimeUnit.NANOSECONDS)
      Thread.sleep(1000)
    }

    writer.close()
  }

  def muxVideoWithAAC_mp4parser() {
    val videoFile = filenameVideoNoAudio
    val audioInputStream = new FileInputStream(filenameAudioFinal)
    val fos = new FileOutputStream(new File(filenameVideoFinal))

    val video = MovieCreator.build(new FileInputStream(videoFile).getChannel)
    val aacTrack = new AACTrackImpl(/*inputStream*/ audioInputStream)
    video.addTrack(aacTrack)
    val out = new DefaultMp4Builder().build(video)
    out.getBox(fos.getChannel)
    fos.close()
  }
}
