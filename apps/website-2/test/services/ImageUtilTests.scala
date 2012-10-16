package services

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File
import play.api.Play
import utils.EgraphsUnitTest

import play.api.test._
import play.api.test.Helpers._


class ImageUtilTests extends EgraphsUnitTest {
  def imageUtil = AppConfig.instance[ImageUtil]

  /*
  it should "test parseSignatureRawCaptureJSON" in {
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
    val photoImage: BufferedImage = ImageIO.read(new File("test/files/longoria/product-2.jpg"))
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(
      TestConstants.signatureStr, Some(TestConstants.messageStr)
    )
    val image: BufferedImage = imageUtil.createEgraphImage(signatureImage, photoImage, 0, 0)
    val combinedImageFile = Play.getFile("test/files/egraph.jpg")
    ImageIO.write(image, "JPG", combinedImageFile)
    combinedImageFile.length should not be (0)
  }

  "createSignatureImage" should "draw a single-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000]],\n   \"y\": [[198.000000]],\n   \"t\": [[13331445844472]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = new File("test/files/single-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(7477)
  }

  "createSignatureImage" should "draw a two-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000,95.148125]],\n   \"y\": [[198.000000,208.518494]],\n   \"t\": [[13331445844472,13331448640856]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = new File("test/files/two-point-stroke.jpg")
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
  */
  it should "resize PNG images correctly" in new EgraphsTestApplication {
    val image = ImageIO.read(resourceFile("image.jpg"))
    val scaled = imageUtil.getScaledInstance(image, 100, 100)
    (scaled.getWidth, scaled.getHeight) should be((100, 100))
  }

  "getDimensions" should "return width and height of image" in new EgraphsTestApplication {
    val dimensions = ImageUtil.getDimensions(resourceFile("image.jpg")).get
    dimensions.width should be(307)
    dimensions.height should be(299)

    ImageUtil.getDimensions(null) should be(None) // null case
    ImageUtil.getDimensions(resourceFile("doesnotexist")) should be(None) // empty file
    ImageUtil.getDimensions(resourceFile("8khz.wav")) should be(None) // Not an image file*/
  }

  "crop" should "crop image to specified dimensions" in new EgraphsTestApplication {
    val originalImage: BufferedImage = ImageIO.read(resourceFile("image.jpg"))
    val croppedImage: BufferedImage = ImageUtil.crop(originalImage, Dimensions(200, 200))
    croppedImage.getWidth should be(200)
    croppedImage.getHeight should be(200)
  }

  it should "retain an image's alpha layer" in {
    val originalImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
    val cropped = ImageUtil.crop(originalImage, Dimensions(50, 50))

    cropped.getType should be(BufferedImage.TYPE_INT_ARGB)
  }

  "isLandscape" should "return true if landscape and false otherwise" in {
    Dimensions(width = 100, height = 1).isLandscape should be(true)
    Dimensions(width = 100, height = 100).isLandscape should be(true)
    Dimensions(width = 1, height = 100).isLandscape should be(false)
  }
}
