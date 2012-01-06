package libs

import java.awt.image.BufferedImage
import util.parsing.json.JSON
import java.awt.geom.Ellipse2D
import javax.imageio.ImageIO
import models.ImageAsset

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.awt.{Transparency, Graphics, RenderingHints, Graphics2D, Color}

object ImageUtil {

  // TODO(wchan): Why does it run out of memory above 5?
  val scaleFactor = 2.5
  val height = 768
  val width = 1024
  val numIncrements = 1000

  def createEgraphImage(signatureImage: BufferedImage, photoImage: BufferedImage, x: Int = 0, y: Int = 0): BufferedImage = {
    val w: Int = scala.math.max(photoImage.getWidth, signatureImage.getWidth)
    val h: Int = scala.math.max(photoImage.getHeight, signatureImage.getHeight)
    val egraphImage: BufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g: Graphics = egraphImage.getGraphics
    g.drawImage(photoImage, 0, 0, null)
    g.drawImage(signatureImage, x, y, null)
    egraphImage
  }

  def createSignatureImage(jsonStr: String): BufferedImage = {
    val strokeData = ImageUtil.parseSignatureRawCaptureJSON(jsonStr)
    val originalXsByStroke = strokeData._1
    val originalYsByStroke = strokeData._2

    val image: BufferedImage = new BufferedImage((width * scaleFactor).intValue(), (height * scaleFactor).intValue(), BufferedImage.TYPE_INT_ARGB)
    val g: Graphics2D = image.getGraphics.asInstanceOf[Graphics2D]
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

    // Draw the shadow
    g.setColor(java.awt.Color.black)
    for (i <- 0 until originalXsByStroke.size) {
      val xs = originalXsByStroke(i).map( penX => penX + 2 ).toArray
      val ys = originalYsByStroke(i).map( penY => penY - 3 ).toArray
      drawStroke(g, xs, ys)
    }

    // Draw the pen
    g.setColor(java.awt.Color.white)
    for (i <- 0 until originalXsByStroke.size) {
      val xs = originalXsByStroke(i).toArray
      val ys = originalYsByStroke(i).toArray
      drawStroke(g, xs, ys)
    }


    getScaledInstance(image, (image.getWidth / 1.8).toInt, (image.getHeight / 1.8).toInt)
  }

  /**
   * @return 3-tuple of originalX data by stroke, originalY data by stroke, and time data by stroke
   */
  def parseSignatureRawCaptureJSON(jsonStr: String): (List[List[Double]], List[List[Double]], List[List[Double]]) = {
    val json: Option[Any] = JSON.parseFull(jsonStr)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[List[Double]]]
    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[List[Double]]]
    val tsByStroke = map.get("time").get.asInstanceOf[List[List[Double]]]
    (originalXsByStroke, originalYsByStroke, tsByStroke)
  }

  /**
   * Draws strokes as a series of partial cubic Bezier curves. Each cubic Bezier is treated as having three segments
   * that defined by points approximately near the control points. drawFirstCubicBezierSegment is called to draw the
   * first of these cubic Bezier segments, and drawSecondAndThirdCubicBezierSegments is called to draw the second and
   * third segments of the last cubic Bezier curve that completes the stroke.
   */
  private def drawStroke(g: Graphics2D, xs: Array[Double], ys: Array[Double], ts: Array[Double] = null) {
    require(xs.size == ys.size, "Xs and Ys must be of same length")
    val n = xs.size

    // TODO(wchan): handle these cases
    if (n < 4) return

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
      // issue#7: calcPointSize based on ts
      drawPoint(g, drawX, (height * scaleFactor).intValue() - drawY)
    }
  }

  private def drawSecondAndThirdCubicBezierSegments(g: Graphics2D, bezier: BezierCubic, tNearC0: Double = 1d / 3d) {
    for (j <- 0 until numIncrements) {
      val drawX = bezier.calcXCoord(tNearC0 + (1d - tNearC0) * j / (2 * numIncrements))
      val drawY = bezier.calcYCoord(tNearC0 + (1d - tNearC0) * j / (2 * numIncrements))
      // issue#7: calcPointSize based on ts
      drawPoint(g, drawX, (height * scaleFactor).intValue() - drawY)
    }
  }

  private def drawPoint(g: Graphics2D, x: Double, y: Double, pointSize: Int = 4) {
    //    g.drawImage(penTipImage, x.intValue(), y.intValue(), null)
    val t = (pointSize * scaleFactor).intValue()
    g.draw(new Ellipse2D.Double(x, y, t, t))
  }

  /**
   * Downscaling code interpreted into Scala from
   * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
   *
   * Convenience method that returns a scaled instance of the
   * provided {@code BufferedImage}.
   *
   * @param img the original image to be scaled
   * @param targetWidth the desired width of the scaled instance,
   *    in pixels
   * @param targetHeight the desired height of the scaled instance,
   *    in pixels
   * @param hint one of the rendering hints that corresponds to
   *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
   *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
   *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
   *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
   * @param higherQuality if true, this method will use a multi-step
   *    scaling technique that provides higher quality than the usual
   *    one-step technique (only useful in downscaling cases, where
   *    {@code targetWidth} or {@code targetHeight} is
   *    smaller than the original dimensions, and generally only when
   *    the {@code BILINEAR} hint is specified)
   * @return a scaled version of the original {@code BufferedImage}
   */
  def getScaledInstance(
    img: BufferedImage,
    targetWidth: Int,
    targetHeight: Int,
    hint: Object = RenderingHints.VALUE_INTERPOLATION_BILINEAR,
    higherQuality: Boolean = true): BufferedImage =
  {
    require(
      targetWidth <= img.getWidth && targetHeight <= img.getHeight,
      "This method should only be used for down-scaling an image"
    )
    var imgType = if (img.getTransparency == Transparency.OPAQUE)
        BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
    var ret: BufferedImage = img
    var w:Int = 0
    var h:Int = 0
    if (higherQuality) {
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        w = img.getWidth
        h = img.getHeight
    } else {
        // Use one-step technique: scale directly from original
        // size to target size with a single drawImage() call
        w = targetWidth
        h = targetHeight
    }

    do {
        if (higherQuality && w > targetWidth) {
            w /= 2
            if (w < targetWidth) {
                w = targetWidth
            }
        }

        if (higherQuality && h > targetHeight) {
            h /= 2
            if (h < targetHeight) {
                h = targetHeight
            }
        }

        var tmp = new BufferedImage(w, h, imgType)
        var g2 = tmp.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint)
        g2.drawImage(ret, 0, 0, w, h, null)
        g2.dispose()

        ret = tmp
    } while (w != targetWidth || h != targetHeight)

    ret
  }

  object Conversions {
    class RichBufferedImage(img: BufferedImage) {
      def asByteArray(imageType: ImageAsset.ImageType) = {
        val bytesOut = new ByteArrayOutputStream()
        ImageIO.write(img, imageType.extension, bytesOut)
        bytesOut.toByteArray
      }
    }

    class ImageEnrichedByteArray(bytes: Array[Byte]) {
      def asBufferedImage: BufferedImage = {
        ImageIO.read(new ByteArrayInputStream(bytes))
      }
    }

    implicit def bufferedImageToRichBufferedImage(img: BufferedImage) = {
      new RichBufferedImage(img)
    }

    implicit def byteArrayToImageEnrichedByteArray(bytes: Array[Byte]): ImageEnrichedByteArray = {
      new ImageEnrichedByteArray(bytes)
    }
  }
}