package services.print

import com.google.zxing.BinaryBitmap
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.awt.image.BufferedImage
import models.ImageAsset
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.EgraphsUnitTest
import services.ImageUtil
import ImageUtil.Conversions._

@RunWith(classOf[JUnitRunner])
class CreateQRTests extends EgraphsUnitTest {

  "generate" should "generate a QR code that encodes the url" in {
    val url = "https://www.egraphs.com/346"
    val qrImage = CreateQR.generate(url)
    decode(qrImage) should be(url)
    qrImage.asByteArray(ImageAsset.Png).length should be(825)
  }

  private def decode(qrImage: BufferedImage): String = {
    val source = new BufferedImageLuminanceSource(qrImage)
    val bitmap = new BinaryBitmap(new HybridBinarizer(source))
    val result = new QRCodeReader().decode(bitmap)
    result.getText
  }
}
