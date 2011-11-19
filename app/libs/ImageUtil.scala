package libs

import java.awt.image.BufferedImage
import util.parsing.json.JSON
import java.awt.{RenderingHints, Graphics2D}
import java.awt.geom.Ellipse2D
import javax.imageio.ImageIO
import java.io.File

object ImageUtil {

  // TODO(wchan): Why does it run out of memory above 5?
  val scaleFactor = 3.0
  val height = 768
  val width = 1024
  val numIncrements = 1000

  val penTip = "public/images/pen_tip_3_70.png"
  val penTipImage: BufferedImage = ImageIO.read(new File(penTip))

  def createSignatureImage(jsonStr: String): BufferedImage = {
    val json: Option[Any] = JSON.parseFull(jsonStr)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[List[Double]]]
    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[List[Double]]]

    val image: BufferedImage = new BufferedImage((width * scaleFactor).intValue(), (height * scaleFactor).intValue(), BufferedImage.TYPE_INT_ARGB)
    val g: Graphics2D = image.getGraphics.asInstanceOf[Graphics2D]
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)

    for (i <- 0 until originalXsByStroke.size) {
      val xs = originalXsByStroke(i).toArray
      val ys = originalYsByStroke(i).toArray
      drawPath(g, xs, ys)
    }

    image
  }

  private def drawPath(g: Graphics2D, xs: Array[Double], ys: Array[Double], ts: Array[Double] = null) {
    val n = xs.size

    // TODO(wchan): handle case if arrays are shorter than length 4. Assert that they are of same length.

    for (i <- 0 until xs.length) {
      xs(i) = xs(i) * scaleFactor
      ys(i) = ys(i) * scaleFactor
    }

    val smoothedXs = xs.clone()
    val smoothedYs = ys.clone()
    for (i <- 0 until n - 3) {
      val bezier = BezierCubic(
        smoothedXs(i), smoothedYs(i),
        xs(i + 1), ys(i + 1),
        xs(i + 2), ys(i + 2),
        xs(i + 3), ys(i + 3))
      drawFirstCubicBezierSegment(g, bezier)

      // replace smoothed point
      val tNearC0: Double = 1d / 3d
      smoothedXs(i + 1) = bezier.calcXCoord(tNearC0)
      smoothedYs(i + 1) = bezier.calcYCoord(tNearC0)
    }

    val i = n - 4
    val bezier = BezierCubic(
      smoothedXs(i), smoothedYs(i),
      smoothedXs(i + 1), smoothedYs(i + 1),
      smoothedXs(i + 2), smoothedYs(i + 2),
      smoothedXs(i + 3), smoothedYs(i + 3))
    drawSecondAndThirdCubicBezierSegments(g, bezier)
  }

  private def drawFirstCubicBezierSegment(g: Graphics2D, bezier: BezierCubic, tNearC0: Double = 1d / 3d) {
    for (j <- 0 until numIncrements) {
      val drawX = bezier.calcXCoord(tNearC0 * j / numIncrements)
      val drawY = bezier.calcYCoord(tNearC0 * j / numIncrements)
      // TODO(wchan): calcPointSize based on ts
      drawPoint(g, drawX, (height * scaleFactor).intValue() - drawY)
    }
  }

  private def drawSecondAndThirdCubicBezierSegments(g: Graphics2D, bezier: BezierCubic, tNearC0: Double = 1d / 3d) {
    for (j <- 0 until numIncrements) {
      val drawX = bezier.calcXCoord(tNearC0 + (1d - tNearC0) * j / (2 * numIncrements))
      val drawY = bezier.calcYCoord(tNearC0 + (1d - tNearC0) * j / (2 * numIncrements))
      // TODO(wchan): calcPointSize based on ts
      drawPoint(g, drawX, (height * scaleFactor).intValue() - drawY)
    }
  }

  private def drawPoint(g: Graphics2D, x: Double, y: Double, pointSize: Int = 4) {
    //    g.drawImage(penTipImage, x.intValue(), y.intValue(), null)
    val t = (pointSize * scaleFactor).intValue()
    g.draw(new Ellipse2D.Double(x, y, t, t))
  }

}