package services.graphics

import utils.{TestConstants, EgraphsUnitTest}
import java.io.FileOutputStream
import org.apache.commons.io.IOUtils
import java.awt.BasicStroke


class HandwritingTests extends EgraphsUnitTest {
  "Handwriting" should "import test samples without exception" in {
    Handwriting(TestConstants.signatureStr)
    Handwriting(TestConstants.messageStr)
  }

  it should "render to graphics" in {
    val source = SVGZGraphicsSource(1200, 1200)
    val graphics = source.graphics
    graphics.setColor(java.awt.Color.gray)
    graphics.fillRect(0, 0, 1200, 1200)

    graphics.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
    graphics.setColor(java.awt.Color.white)

    val writing = Handwriting(TestConstants.signatureStr).append(Handwriting(TestConstants.messageStr))
    val pen = HandwritingPen()

    // pen.copy(color=Color.black).translatingBy(3.0, 3.0).write(writing, graphics)
    pen.write(writing, graphics)

    println("Serializing to file")

    IOUtils.write(
      source.asByteArray,
      new FileOutputStream("/tmp/sig." + source.fileExtension)
    )
    println("Done serializing")
  }
}