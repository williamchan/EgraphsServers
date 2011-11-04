import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec

class ImageTest extends UnitFlatSpec
with ShouldMatchers {

  it should "overlay PNG images correctly" in {
    val path: File = new File("test/files")

    // load source images
    val image: BufferedImage = ImageIO.read(new File(path, "image.png"))
    val overlay: BufferedImage = ImageIO.read(new File(path, "overlay.png"))

    // create the new image, canvas size is the max. of both image sizes
    val w: Int = scala.math.max(image.getWidth, overlay.getWidth)
    val h: Int = scala.math.max(image.getHeight, overlay.getHeight)
    val combinedImage: BufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

    // paint both images, preserving the alpha channels
    val g: Graphics = combinedImage.getGraphics
    g.drawImage(image, 0, 0, null)
    g.drawImage(overlay, 0, 0, null)

    // Save as new image
    val combined: File = new File(path, "combinedImage.png")
    ImageIO.write(combinedImage, "PNG", combined)
    combined.length should be(149657)
  }
}
