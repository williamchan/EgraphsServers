package services.graphics

import java.io.FileOutputStream
import org.apache.commons.io.IOUtils
import java.awt.BasicStroke
import utils.{ClearsDatabaseAndValidationBefore, TestConstants, EgraphsUnitTest}


class HandwritingTests extends EgraphsUnitTest with ClearsDatabaseAndValidationBefore {

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

  "translatingBy" should "translatingBy" in {
    val writing = Handwriting(TestConstants.shortWritingStr)
    val translated = writing.translatingBy(xOffset = 100.toDouble, yOffset = 100.toDouble)
    translated.strokes.head.points.head.x should be (167.0)
    translated.strokes.head.points.head.y should be (298.0)
  }

  "scalingBy" should "scalingBy" in {
    val writing = Handwriting(TestConstants.shortWritingStr)
    val translated = writing.scalingBy(scaleFactor = 2.0)
    translated.strokes.head.points.head.x should be (134.0)
    translated.strokes.head.points.head.y should be (396.0)
  }
}