package libs


case class BezierCubic(p0x: Double, p0y: Double,
                       c0x: Double, c0y: Double,
                       c1x: Double, c1y: Double,
                       p1x: Double, p1y: Double) {

  def calculateTClosestToC0(increments: Int = 1000): Double = {
    var closestT: Double = 0d
    var closestDistance: Double = Double.MaxValue

    for (i <- 0 until increments) {
      val currentT = int2double(i) / increments
      val currentDistance = BezierCubic.distance(c0x, c0y, calcXCoord(currentT), calcYCoord(currentT))
      if (currentDistance < closestDistance) {
        closestDistance = currentDistance
        closestT = currentT
      }
      else {
        return closestT
      }
    }

    // TODO(wchan): Should throw exception
    closestT
  }

  def calcXCoord(t: Double): Double = {
    ((math.pow(1d - t, 3) * p0x)
      + (3 * math.pow(1d - t, 2) * t * c0x)
      + (3 * (1d - t) * math.pow(t, 2) * c1x)
      + (math.pow(t, 3) * p1x))
  }

  def calcYCoord(t: Double): Double = {
    ((math.pow(1d - t, 3) * p0y)
      + (3 * math.pow(1d - t, 2) * t * c0y)
      + (3 * (1d - t) * math.pow(t, 2) * c1y)
      + (math.pow(t, 3) * p1y))
  }
}

object BezierCubic {

  def distance(x0: Double, y0: Double, x1: Double, y1: Double): Double = {
    math.sqrt(math.pow(x1 - x0, 2) + math.pow(y1 - y0, 2))
  }
}