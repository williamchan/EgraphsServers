package services.graphics

import java.awt.geom.CubicCurve2D

/**
 * A cubic Bezier curve defined by two end points and a pair of control points.
 **/
case class BezierCubic(
  p0x: Double, p0y: Double,
  c0x: Double, c0y: Double,
  c1x: Double, c1y: Double,
  p1x: Double, p1y: Double
) {

  /** Returns the X and Y coordinate of the curve evaluated at t */
  def apply(t: Double): Point = {
    Point(
      x=calcCoord(t, p0x, c0x, c1x, p1x),
      y=calcCoord(t, p0y, c0y, c1y, p1y)
    )
  }

  /** Returns an iterable of the curve's control points */
  def controlPoints: Iterable[(Double, Double)] = {
    List((p0x, p0y), (c0x, c0y), (c1x, c1y), (p1x, p1y))
  }

  /**
   * Splits the bezier into two halves at the point 0.0 <= t <= 1.0 .
   *
   * @return a tuple of the BezierCubic evaluated from 0 -> t and the one evaluated from t -> 1.0
   **/
  def split(t: Double): (BezierCubic, BezierCubic) = {
    require(t >= 0.0 && t <= 1.0, "t needs to be between 0 and 1 inclusive")

    val thisCurve = new CubicCurve2D.Double(p0x, p0y, c0x, c0y, c1x, c1y, p1x, p1y)
    val firstHalf = new CubicCurve2D.Double()
    val secondHalf = new CubicCurve2D.Double()

    BezierUtils.splitCurve(thisCurve, t, firstHalf, secondHalf)

    (BezierCubic(firstHalf), BezierCubic(secondHalf))
  }

  private def calcCoord(t: Double, p0: Double, c0: Double, c1: Double, p1: Double): Double = {
    ((math.pow(1d - t, 3) * p0)
      + (3 * math.pow(1d - t, 2) * t * c0)
      + (3 * (1d - t) * math.pow(t, 2) * c1)
      + (math.pow(t, 3) * p1))
  }
}


object BezierCubic {
  /** Creates a BezierCubic from its sister class in java.awt */
  def apply(curve: CubicCurve2D): BezierCubic = {
    BezierCubic(
      curve.getX1, curve.getY1,
      curve.getCtrlX1, curve.getCtrlY1,
      curve.getCtrlX2, curve.getCtrlY2,
      curve.getX2, curve.getY2
    )
  }
}
