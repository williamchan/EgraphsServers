package services.print

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage

/**
 * Using a QR code image scanner, a user should be able to access the url encoded in the image. This should work on
 * iOS, Android, Windows, and other major mobile devices.
 *
 * This implementation of a QR image generation is based on com.google.zxing.client.j2se.CommandLineEncoder.
 */
object CreateQR {

  private val h = 200
  private val w = 200

  def generate(url: String): BufferedImage = {
    val writer = new QRCodeWriter()
    val matrix = writer.encode(url, BarcodeFormat.QR_CODE, w, h)
    MatrixToImageWriter.toBufferedImage(matrix)
  }
}
