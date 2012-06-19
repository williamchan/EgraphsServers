package services.graphics

/**
 * A rectangle in a coordinate system
 */
case class Rectangle(x: Double, y: Double, width: Double, height: Double) {
  def scaled(scaleFactor: Double): Rectangle = {
    copy(
      scaleFactor * x,
      scaleFactor * y,
      scaleFactor * width,
      scaleFactor * height
    )
  }
}

