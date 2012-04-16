package services

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import java.io._
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import play.Play
import utils.TestConstants
import collection.immutable.List
import org.apache.batik.dom.GenericDOMImplementation
import java.awt.geom.{Ellipse2D, Path2D}
import java.awt.{RenderingHints, BasicStroke, Graphics2D}
import services.graphics.BezierCubic

class ImageUtilTests extends UnitFlatSpec
with ShouldMatchers {
  def imageUtil = AppConfig.instance[ImageUtil]

/*  it should "test parseSignatureRawCaptureJSON" in {
    val strokeData = imageUtil.parseSignatureRawCaptureJSON(TestConstants.signatureStr)
    val xsByStroke = strokeData._1
    val ysByStroke = strokeData._2
    val timesByStroke = strokeData._3
    val numStroke = xsByStroke.size

    for (i <- 0 until numStroke) {
      xsByStroke(i).isInstanceOf[List[Number]] should be(true)
      ysByStroke(i).isInstanceOf[List[Number]] should be(true)
      timesByStroke(i).isInstanceOf[List[Number]] should be(true)
    }
  }

  it should "overlay Andrew's signature on a JPG" in {
    val photoImage: BufferedImage = ImageIO.read(Play.getFile("test/files/longoria/product-2.jpg"))
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(
      TestConstants.signatureStr, Some(TestConstants.messageStr)
    )
    val egraphImage: BufferedImage = imageUtil.createEgraphImage(signatureImage, photoImage, 0, 0)
    val combinedImageFile = Play.getFile("test/files/egraph.jpg")
    ImageIO.write(egraphImage, "JPG", combinedImageFile)
    combinedImageFile.length should not be (0)
  }

  "createSignatureImage" should "draw a single-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000]],\n   \"y\": [[198.000000]],\n   \"t\": [[13331445844472]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = Play.getFile("test/files/single-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(7477)
  }

  "createSignatureImage" should "draw a two-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000,95.148125]],\n   \"y\": [[198.000000,208.518494]],\n   \"t\": [[13331445844472,13331448640856]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = Play.getFile("test/files/two-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(8367)
  }

  "createSignatureImage" should "draw a three-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000,95.148125,121.414230]],\n   \"y\": [[198.000000,208.518494,226.561005]],\n   \"t\": [[13331445844472,13331448640856,13331448883353]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = Play.getFile("test/files/three-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(9239)
  }

  it should "overlay PNG images correctly" in {
    val image: BufferedImage = ImageIO.read(Play.getFile("test/files/image.png"))
    val overlay: BufferedImage = ImageIO.read(Play.getFile("test/files/overlay.png"))
    val combinedImage = imageUtil.createEgraphImage(overlay, image, 0, 0)
    val combinedImageFile: File = Play.getFile("test/files/combinedImage.png")
    ImageIO.write(combinedImage, "PNG", combinedImageFile)
    combinedImageFile.length should be(138807)
  }

  it should "resize PNG images correctly" in {
    val image = ImageIO.read(Play.getFile("test/files/image.png"))
    val scaled = imageUtil.getScaledInstance(image, 100, 100)
    (scaled.getWidth, scaled.getHeight) should be((100, 100))
  }

  "getDimensions" should "return width and height of image" in {
    val dimensions = ImageUtil.getDimensions(Play.getFile("test/files/image.png")).get
    dimensions.width should be (307)
    dimensions.height should be (299)

    ImageUtil.getDimensions(null) should be (None)                                // null case
    ImageUtil.getDimensions(Play.getFile("doesnotexist")) should be (None)        // empty file
    ImageUtil.getDimensions(Play.getFile("test/files/8khz.wav")) should be (None) // Not an image file
  }

  "getCropDimensions" should "crop to ideal dimensions when image is landscape (width > height)" in {
    val noCrop = ImageUtil.getCropDimensions(Dimensions(width = 1400, height = 1000))
    noCrop.width should be (1400)
    noCrop.height should be (1000)

    val cropWidthOff = ImageUtil.getCropDimensions(Dimensions(width = 2000, height = 1000))
    cropWidthOff.width should be (1400)
    cropWidthOff.height should be (1000)

    val cropHeightOff = ImageUtil.getCropDimensions(Dimensions(width = 1400, height = 1200))
    cropHeightOff.width should be (1400)
    cropHeightOff.height should be (1000)
  }

  "getCropDimensions" should "crop to ideal dimensions when image is portrait (height > width)" in {
    val noCrop = ImageUtil.getCropDimensions(Dimensions(width = 718, height = 1000))
    noCrop.width should be (718)
    noCrop.height should be (1000)

    val cropHeightOff = ImageUtil.getCropDimensions(Dimensions(width = 900, height = 1000))
    cropHeightOff.width should be (718)
    cropHeightOff.height should be (1000)

    val cropWidthOff = ImageUtil.getCropDimensions(Dimensions(width = 718, height = 1200))
    cropWidthOff.width should be (718)
    cropWidthOff.height should be (1000)
  }

  "crop" should "crop image to specified dimensions" in {
    val originalImage: BufferedImage = ImageIO.read(Play.getFile("test/files/image.png"))
    val croppedImage: BufferedImage = ImageUtil.crop(originalImage, Dimensions(200, 200))
    croppedImage.getWidth should be (200)
    croppedImage.getHeight should be (200)
  }
  
  it should "retain an image's alpha layer" in {
    val originalImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
    val cropped = ImageUtil.crop(originalImage, Dimensions(50, 50))

    cropped.getType should be (BufferedImage.TYPE_INT_ARGB)
  }

  "getMaxDouble" should "get Max in list of list of Doubles" in {
    ImageUtil.getMaxDouble(List()) should be(None)
    ImageUtil.getMaxDouble(List(List(1.0, 2.0, 3.0))).get should be(3.0)
    ImageUtil.getMaxDouble(List(List(1.0, 2.0, 3.0), List(), List(1.0, 20.0, 3.0))).get should be(20.0)
  }*/
}
