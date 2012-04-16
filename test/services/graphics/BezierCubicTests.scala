package services.graphics

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import java.io.File
import java.awt.{Graphics2D, RenderingHints, BasicStroke}
import java.awt.image.BufferedImage
import java.awt.geom.{Ellipse2D, Path2D}
import play.Play
import javax.imageio.ImageIO

class BezierCubicTests extends UnitFlatSpec
with ShouldMatchers {

  it should "evaluate to the correct x and y coordinates" in {
    val bezier = BezierCubic(
      0d, 0d,
      0d, 4d,
      4d, 4d,
      4d, 0d
    )
    bezier(0) should be(Point(0, 0))
    bezier(.25) should be(Point(0.625, 2.25))
    bezier(.5) should be(Point(2, 3))
    bezier(.75) should be(Point(3.375, 2.25))
    bezier(1) should be(Point(4, 0))
  }

  "BezierCubic" should "derive correct sub-curves" in {
    import java.awt.Color

    val orig = BezierCubic(
      100, 100,
      250, 0,
      275, 200,
      300, 100
    )

    val (firstHalf, secondHalf) = orig.split(0.5)

    // Draw the two halves
    val image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB)
    val g = image.getGraphics.asInstanceOf[Graphics2D]
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val toDraw = List(
      (orig, Color.green, 5.0f),
      (firstHalf, Color.red, 2.0f),
      (secondHalf, Color.blue, 2.0f)
    )

    g.setStroke(new BasicStroke(4.0f))
    for ((curve, color, stroke) <- toDraw) {
      g.setColor(color)
      g.setStroke(new BasicStroke(stroke))
      val path = new Path2D.Double()
      path.moveTo(curve.p0x, curve.p0y)
      path.curveTo(
        curve.c0x, curve.c0y,
        curve.c1x, curve.c1y,
        curve.p1x, curve.p1y
      )
      g.draw(path)

      for ((x, y) <- curve.controlPoints) {
        g.fill(new Ellipse2D.Double(x, y, 6, 6))
      }
    }


    g.dispose()

    ImageIO.write(image, "PNG", Play.getFile("/tmp/files/curvetest.png"))

    // Check to make sure calculations make sense
    firstHalf(1.0) should be (orig(0.5))
    firstHalf(0.0) should be (orig(0.0))
    secondHalf(0.0) should be (orig(0.5))
    secondHalf(1.0) should be (orig(1.0))
  }
}