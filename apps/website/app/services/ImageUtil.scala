package services

import java.awt.image.BufferedImage
import com.google.inject.Inject

import javax.imageio.stream.ImageInputStream
import javax.imageio.{ImageReader, ImageIO}
import java.awt._
import java.io.{File, ByteArrayInputStream, ByteArrayOutputStream}
import models.ImageAsset

@Inject() class ImageUtil {
  import ImageUtil._

  /**
   * Downscaling code interpreted into Scala from
   * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
   *
   * Convenience method that returns a scaled instance of the
   * provided [[java.awt.image.BufferedImage]].
   *
   * @param img the original image to be scaled
   * @param targetWidth the desired width of the scaled instance,
   *    in pixels
   * @param targetHeight the desired height of the scaled instance,
   *    in pixels
   * @param hint one of the rendering hints that corresponds to
   * { @code RenderingHints.KEY_INTERPOLATION} (e.g.
   * { @code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
   * { @code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
   * { @code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
   * @param higherQuality if true, this method will use a multi-step
   *    scaling technique that provides higher quality than the usual
   *    one-step technique (only useful in downscaling cases, where
   * { @code targetWidth} or { @code targetHeight} is
   *    smaller than the original dimensions, and generally only when
   *    the { @code BILINEAR} hint is specified)
   * @return a scaled version of the original { @code BufferedImage}
   */
  def getScaledInstance(img: BufferedImage,
                        targetWidth: Int,
                        targetHeight: Int,
                        hint: Object = RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                        higherQuality: Boolean = true): BufferedImage = {
    require(
      targetWidth <= img.getWidth && targetHeight <= img.getHeight,
      "This method should only be used for down-scaling an image"
    )
    val imgType = derivativeImageTypeFromSourceImage(img)
    var ret: BufferedImage = img
    var w: Int = 0
    var h: Int = 0
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

      val tmp = new BufferedImage(w, h, imgType)
      val g2 = tmp.createGraphics()
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint)
      g2.drawImage(ret, 0, 0, w, h, null)
      g2.dispose()

      ret = tmp
    } while (w != targetWidth || h != targetHeight)

    ret
  }
}

object ImageUtil extends ImageUtil {
  /**
   * Crops the provided image into a square whose length on each side is
   * the minimum of the provided image's width and height
   **/
  def cropToSquare(image: BufferedImage) = {
    val minorDimension = scala.math.min(image.getWidth, image.getHeight)
    crop(image, Dimensions(minorDimension, minorDimension))
  }

  /**
   * Attempts to open the image located at File. Returns Some(the image) if successful
   * otherwise returns None.
   *
   * @param imageFile
   */
  def parseImage(imageFile: File): Option[BufferedImage] = {
    try {
      Some(ImageIO.read(imageFile))
    } catch {
      case e: Exception => {
        Utils.logException(e)
        None
      }
    }
  }

  def getDimensions(imageFile: File): Option[Dimensions] = {
    if (imageFile == null || imageFile.length() == 0) return None

    val in: ImageInputStream = ImageIO.createImageInputStream(imageFile)
    try {
      val readers = ImageIO.getImageReaders(in)
      if (readers.hasNext) {
        val reader: ImageReader = readers.next().asInstanceOf[ImageReader]
        reader.setInput(in)
        Some(Dimensions(width = reader.getWidth(0), height = reader.getHeight(0)))
      } else {
        None
      }
    } finally {
      if (in != null) in.close()
    }
  }

  def crop(originalImage: BufferedImage, cropDimensions: Dimensions): BufferedImage = {
    val croppedImage = new BufferedImage(
      cropDimensions.width,
      cropDimensions.height,
      derivativeImageTypeFromSourceImage(originalImage)
    )
    val g: Graphics = croppedImage.getGraphics
    g.drawImage(originalImage, 0, 0, null)
    croppedImage
  }

  /**
   * Returns the BufferedImage.TYPE_ value that should be used when making a derivative image of some
   * image that already exists (e.g., if resizing or cropping the image)
   *
   * @param image the source image from which some new image will be derived
   * @return the type the new BufferedImage should use.
   */
  private def derivativeImageTypeFromSourceImage(image: BufferedImage): Int = {
    image.getTransparency match {
      case Transparency.OPAQUE => BufferedImage.TYPE_INT_RGB
      case _ => BufferedImage.TYPE_INT_ARGB
    }
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

case class Dimensions(width: Int, height: Int) {
  def isLandscape: Boolean = width > height
}
