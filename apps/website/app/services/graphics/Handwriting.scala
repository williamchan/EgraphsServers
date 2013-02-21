package services.graphics

import util.parsing.json.JSON
import java.awt.geom.AffineTransform
import RichGraphicsConversions._
import java.awt.{Color, BasicStroke, Graphics2D}
import services.logging.Logging

/**
 * Represents one or many strokes of handwriting stored in our proprietary vector format. Provides
 * the functionality to draw the strokes to a canvas and implements the algorithm that we use to
 * create a smooth curve out of the sampled values from the iPad.
 *
 * @param strokes the set of strokes represented by the Handwriting.
 * @param transform the transform that should be used on the drawn handwriting. This defaults to
 *   no transform.
 */
case class Handwriting(strokes: Seq[HandwritingStroke], transform: AffineTransform=new AffineTransform()) {

  /**
   * Renders the Handwriting onto the provided Java2D canvas.
   *
   * @param graphics
   */
  def write(graphics: Graphics2D) {
    strokes.foreach(stroke => stroke.write(transform, graphics))
  }

  /**
   * Returns a Handwriting comprised of the strokes from this handwriting AND the strokes from another
   * Handwriting.
   *
   * @param otherWriting the handwriting whose strokes should be included in the returned
   *     Handwriting
   * @return the new Handwriting with strokes from both previous Handwritings
   */
  def append(otherWriting: Handwriting): Handwriting = {
    copy(strokes=strokes ++ otherWriting.strokes)
  }

  /**
   * Returns a Handwriting translated by (x, y) points from the top and left, respectively.
   *
   * @param xOffset The number of points by which to translate from the left of the drawing origin
   * @param yOffset The number of points by which to translate from the top of the drawing origin
   *
   * @return a copy of the Handwriting that will draw its strokes translated by (x, y) pixels.
   */
  def translatingBy(xOffset: Double, yOffset: Double): Handwriting = {
    val newStrokes = for (stroke <- strokes) yield {
      val newPoints = for (point <- stroke.points) yield {
        point.copy(x= point.x + xOffset, y= point.y + yOffset)
      }

      new HandwritingStroke(newPoints)
    }

    copy(strokes=newStrokes)
  }

  /**
   * Returns a Handwriting scaled proportionally in size by scaleFactor.
   *
   * For example, scalingBy(2.0) would return a Handwriting that renders at twice the size of
   * the original.
   *
   * @param scaleFactor The amount by which to scale it (0.0 -> n)
   * @return a copy of the Handwriting that will draw its strokes translated by (x, y) pixels.
   */
  def scalingBy(scaleFactor: Double): Handwriting = {
    val newStrokes = for (stroke <- strokes) yield {
      val newPoints = for (point <- stroke.points) yield {
        point.copy(x= point.x * scaleFactor, y= point.y * scaleFactor)
      }

      new HandwritingStroke(newPoints)
    }

    copy(strokes=newStrokes)
  }
}


object Handwriting extends Logging {

  val defaultPenWidth: Double = 5.0
  val defaultShadowOffsetX: Double = 3.0
  val defaultShadowOffsetY: Double = 3.0

  /**
   * Creates a Handwriting object out of our internally used JSON format. See that format's
   * [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints documentation]]
   *
   * @param jsonText the JSON string that should be translated into Handwriting
   * @return the Handwriting from the JSON String.
   */
  def apply(jsonText: String): Handwriting = {
    JSON.parseFull(jsonText) match {
      case Some(strokeListsTypeErased) =>
        val strokeLists = strokeListsTypeErased.asInstanceOf[Map[String, List[List[Double]]]]
        val strokePointLists = List(
          strokeLists("x"),
          strokeLists("y"),
          strokeLists("t")
        ).transpose

        val strokes = for (strokePoints <- strokePointLists) yield {
          HandwritingStroke(strokePoints)
        }

        Handwriting(strokes)

      case None =>
        throw new IllegalArgumentException("Invalid handwriting JSON: " + jsonText)
    }
  }
}

/**
 * A single stroke of handwriting; all the writing that occurs between a pen-down and pen-up.
 *
 * @param points the points from the stroke
 */
class HandwritingStroke(val points: Seq[HandwritingPoint]) {
  /**
   * Draws the stroke onto the Java2D graphics canvas with the given transform.
   *
   * @param transform
   * @param graphics
   */
  def write(transform: AffineTransform, graphics: Graphics2D) {
    if (!points.isEmpty) graphics.drawPath { path =>
      val percentBezierToDraw = 1d / 3d

      // Move to the beginning of the stroke
      path.moveTo(points.head.x, points.head.y)

      // Populate the path by moving a 4-point sliding window along the stroke points.
      for (pointGroup <- points.tail.sliding(3)) {
        pointGroup match {
          // >= 4 points in the stroke. Cubic Bezier. This is the most common case.
          case Seq(p1, p2, p3) =>
            val bezier = BezierCubic(
              path.getCurrentPoint.getX, path.getCurrentPoint.getY,
              p1.x, p1.y,
              p2.x, p2.y,
              p3.x, p3.y
            )

            val curveToDraw = bezier.split(t=percentBezierToDraw)._1
            path.curveTo(
              curveToDraw.c0x, curveToDraw.c0y,
              curveToDraw.c1x, curveToDraw.c1y,
              curveToDraw.p1x, curveToDraw.p1y
            )

          // Three points in the stroke. Draw a quadratic bezier
          case Seq(p1, p2) =>
            path.quadTo(p1.x, p1.y, p2.x, p2.y)

          // Two points in the stroke. Draw a line between them.
          case Seq(p1) =>
            path.lineTo(p1.x, p1.y)

          // One point in the stroke. Draw a line to self. With rounded caps this should
          // appear circular.
          case Seq() =>
            path.lineTo(points.head.x, points.head.y)

          case anythingElse =>
            throw new IllegalArgumentException(
              "Unable to draw " + anythingElse + " derived from points: " + points
            )
        }
      }

      // Transform the path before giving it back to the Graphics to draw.
      path.transform(transform)
    }
  }
}


object HandwritingStroke {
  /**
   * Creates a HandwritingStroke from a sequence of Seq(x, y, t)s. For example (in json format)
   * {{{
   *   val stroke = HandwritingStroke(Seq(
   *     Seq(0, 0, 1),  // x coordinates
   *     Seq(0, 0, 30), // y coordinates
   *     Seq(0, 10, 20) // t coordinates
   *   ))
   *
   * }}}
   *
   * @param pointLists
   * @return
   */
  def apply(pointLists: Seq[Seq[Double]]): HandwritingStroke = {
    // Iterate through the transposed points; transposal gives us the points individually
    val handwritingPoints = for (pointList <- pointLists.transpose) yield {
      pointList match {
        case Seq(x: Double, y: Double, t: Double) =>
          HandwritingPoint(x, y, t)

        case _ =>
          throw new IllegalArgumentException("Invalid stroke: " + pointList)
      }
    }

    new HandwritingStroke(handwritingPoints)
  }
}


/** A single point of handwriting as gathered by a Celebrity's tablet */
case class HandwritingPoint(x: Double, y: Double, t: Double)


/**
 * Rendering hints for a [[services.graphics.HandwritingPen]] to render a shadow of
 * the handwriting.
 *
 * @param offsetX the offset that the shadow should fall on the X axis
 * @param offsetY the offset that the shadow should fall on the Y axis
 * @param color the color of the shadow.
 */
case class HandwritingShadow(offsetX: Double, offsetY: Double, color: Color)


/**
 * Represents the pen used to write [[services.graphics.Handwriting]].
 *
 * @param width thickness of the pen in pixels
 * @param color color of the pen
 * @param shadowOption if the pen should cast a shadow, what the shadow should look like
 * @param transform any additional transforms that should occur to the pen.
 */
case class HandwritingPen(
  width: Double=Handwriting.defaultPenWidth,
  color: Color=Color.white,
  shadowOption:Option[HandwritingShadow]=Some(HandwritingShadow(Handwriting.defaultShadowOffsetX, Handwriting.defaultShadowOffsetY, Color.black)),
  transform:AffineTransform=new AffineTransform()
) {

  /**
   * Writes [[services.graphics.Handwriting]] onto a Java2D graphics canvas.
   *
   * @param handWriting the handwriting to write.
   * @param graphics the canvas upon which to write the Handwriting.
   */
  def write(handWriting: Handwriting, graphics: Graphics2D) {
    graphics.withStroke(stroke) {
      // Draw the shadow
      for (shadow <- shadowOption) {
        graphics.withColor(shadow.color) {
          handWriting.translatingBy(shadow.offsetX, shadow.offsetY).write(graphics)
        }
      }

      // Draw the stroke
      graphics.withColor(color)(handWriting.write(graphics))
    }
  }

  /**
   * Scales the pen by scaleFactor.
   *
   * For example, scalingStrokeBy(2.0) would produce a pen with twice as much width
   * as the previous pen.
   *
   * @param scaleFactor
   * @return
   */
  def scalingStrokeBy(scaleFactor: Double): HandwritingPen = {
    val newShadowOption = shadowOption.map { shadow =>
      shadow.copy(offsetX=shadow.offsetX * scaleFactor, offsetY=shadow.offsetY * scaleFactor)
    }

    copy(width=width * scaleFactor, shadowOption=newShadowOption)
  }

  private def stroke: BasicStroke = {
    new BasicStroke(width.floatValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  }
}