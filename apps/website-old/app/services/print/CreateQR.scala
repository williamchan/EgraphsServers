package services.print

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.nio.CharBuffer
import java.nio.charset.Charset
import scala.collection.JavaConversions._

/**
 * Inspired by http://stackoverflow.com/questions/2489048/qr-code-encoding-and-decoding-using-zxing
 */
object CreateQR {

  private val charset = Charset.forName("UTF-8")
  private val h = 200
  private val w = 200

  def generate(url: String): BufferedImage = {
    try {
      // Convert a string to UTF-8 bytes in a ByteBuffer
      val encoder = charset.newEncoder()
      val bbuf = encoder.encode(CharBuffer.wrap(url))
      val b = bbuf.array()
      val data = new String(b, "UTF-8")
      val writer = new QRCodeWriter()
      val hints = Map(
        EncodeHintType.CHARACTER_SET -> "UTF-8",
        EncodeHintType.ERROR_CORRECTION -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
      )
      val matrix = writer.encode(data, BarcodeFormat.QR_CODE, w, h, hints)
      MatrixToImageWriter.toBufferedImage(matrix)
    } catch {
      case e: Exception => error(e.getMessage)
    }
  }

}
