package services.graphics

import java.awt.geom.QuadCurve2D

/**
 * A quadratic Bezier curve defined by two end points and a single control point.
 */
case class BezierQuadratic(
  ax: Double, ay: Double,
  bx: Double, by: Double,
  cx: Double, cy: Double
) {

  def split(t: Double): (BezierQuadratic, BezierQuadratic) = {
    require(t >= 0.0 && t <= 1.0, "t needs to be between 0 and 1 inclusive")

    val thisCurve = new QuadCurve2D.Double(ax, ay, bx, by, cx, cy)
    val firstHalf = new QuadCurve2D.Double()
    val secondHalf = new QuadCurve2D.Double()

    BezierUtils.splitCurve(thisCurve, t, firstHalf, secondHalf)

    (BezierQuadratic(firstHalf), BezierQuadratic(secondHalf))

  }
}

object BezierQuadratic {
  def apply(javaCurve: QuadCurve2D): BezierQuadratic = {
    BezierQuadratic(
      javaCurve.getX1, javaCurve.getY1,
      javaCurve.getCtrlX, javaCurve.getCtrlY,
      javaCurve.getX2, javaCurve.getY2
    )
  }
}