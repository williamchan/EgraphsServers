package services.graphics

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io._
import models.ImageAsset
import org.apache.batik.dom.GenericDOMImplementation
import java.awt.{Dimension, Graphics2D}
import java.util.zip.GZIPOutputStream
import services.{Utils, Dimensions}
import org.apache.batik.svggen.{SVGGeneratorContext, SVGGraphics2D}

/**
 * Describes objects that can act as sources for a Java2D drawing canvas. Includes functionality
 * for serializing the canvas when drawing is done.
 */
trait GraphicsSource {
  /** Dimensions of the canvas that will be provided*/
  def dimensions: Dimensions

  /** The canvas to draw on */
  def graphics: Graphics2D

  /** Serializes the canvas into a byte array */
  def asByteArray: Array[Byte]

  /** The file extension that should be used when serializing this canvas */
  def fileExtension: String

  /** Returns a copy of this graphics source with the specified pixel dimensions */
  def withDimensions(width: Double, height: Double): GraphicsSource
}


/**
 * Graphics source that draws onto a raster canvas provided by a BufferedImage.
 */
class RasterGraphicsSource(
  width: Int,
  height: Int,
  imageType: ImageAsset.ImageType=ImageAsset.Png
) extends GraphicsSource
{
  lazy val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  override lazy val graphics = image.getGraphics.asInstanceOf[Graphics2D]

  override def dimensions = {
    Dimensions(width, height)
  }

  def withDimensions(width: Double, height: Double): GraphicsSource = {
    new RasterGraphicsSource(width.toInt, height.toInt, imageType)
  }

  override def asByteArray: Array[Byte] = {
    val writeStream = new ByteArrayOutputStream()

    ImageIO.write(image, imageType.extension, writeStream)

    writeStream.close()
    writeStream.toByteArray
  }

  override def fileExtension: String = {
    imageType.extension
  }
}


/**
 * Graphics source that draws onto a vector canvas provided by a Batik SVG Graphics2D
 * implementation
 */
case class SVGGraphicsSource(width: Int, height: Int) extends GraphicsSource {
  override lazy val graphics: SVGGraphics2D = {
    val elementFactory = GenericDOMImplementation
      .getDOMImplementation
      .createDocument("http://www.w3.org/2000/svg", "svg", null)

    val svgGraphicsContext = SVGGeneratorContext.createDefault(elementFactory)
    svgGraphicsContext.setImageHandler(new ImageHandlerJPEGBase64Encoder(1))
    val graphics = new SVGGraphics2D(svgGraphicsContext, false /* no text as shapes */)
    graphics.setSVGCanvasSize(new Dimension(width, height))

    graphics
  }

  override def dimensions: Dimensions = {
    Dimensions(width, height)
  }

  def withDimensions(width: Double, height: Double): SVGGraphicsSource = {
    new SVGGraphicsSource(width.toInt, height.toInt)
  }

  override def asByteArray: Array[Byte] = {
    val writer = new StringWriter()

    graphics.stream(
      writer,
      true // use CSS
    )

    writer.close()
    writer.toString.getBytes
  }

  override def fileExtension: String = {
    "svg"
  }
}


/**
 * The same as an [[services.graphics.SVGGraphicsSource]], but serializes to a GZIPped .svgz file
 * rather than .svg file.
 *
 * @param svgCanvas the SVGGraphicsSource that should be delegated to for most operations.
 */
class SVGZGraphicsSource(svgCanvas:SVGGraphicsSource) extends GraphicsSource
{
  override def dimensions: Dimensions = {
    svgCanvas.dimensions
  }

  override def withDimensions(width: Double, height: Double): GraphicsSource = {
    new SVGZGraphicsSource(svgCanvas.withDimensions(width, height))
  }

  override def graphics: Graphics2D = {
    svgCanvas.graphics
  }

  override def asByteArray: Array[Byte] = {
    val stringStream = new ByteArrayOutputStream()
    val gzipStream = new GZIPOutputStream(stringStream)
    val writer = new OutputStreamWriter(gzipStream)

    svgCanvas.graphics.stream(
      writer,
      true // use CSS
    )

    writer.close()
    gzipStream.close()
    stringStream.close()

    stringStream.toByteArray
  }

  override def fileExtension: String = {
    "svgz"
  }
}


object SVGZGraphicsSource {
  def apply(width: Int, height: Int): SVGZGraphicsSource = {
    new SVGZGraphicsSource(new SVGGraphicsSource(width, height))
  }
}

object RasterGraphicsSource {
  def apply(width: Int, height: Int): RasterGraphicsSource = {
    new RasterGraphicsSource(width, height)
  }
}
