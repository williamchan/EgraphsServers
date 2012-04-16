package services.graphics

import java.awt.{Color, BasicStroke, Graphics2D, Stroke}
import java.awt.geom.Path2D

class RichGraphics(graphics: Graphics2D) {
  def withStroke[A](stroke: BasicStroke)(operation: => A): A = {
    val originalStroke = graphics.getStroke

    graphics.setStroke(stroke)
    val result = operation
    graphics.setStroke(originalStroke)

    result
  }

  def withColor[A](color: Color)(operation: => A): A = {
    val originalColor = graphics.getColor

    graphics.setColor(color)
    val result = operation
    graphics.setColor(originalColor)

    result
  }

  def drawPath[A](operation: Path2D.Double => A): A = {
    val path = new Path2D.Double()

    val result = operation(path)

    graphics.draw(path)

    result
  }
}

object RichGraphicsConversions {
  implicit def graphicsToRichGraphics(graphics: Graphics2D): RichGraphics = {
    new RichGraphics(graphics)
  }
}