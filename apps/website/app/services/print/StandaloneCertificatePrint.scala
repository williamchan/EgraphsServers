package services.print

import java.awt.image.BufferedImage
import java.util.Date
import javax.imageio.ImageIO
import play.api.Play._
import java.awt.RenderingHints

/**
 * For holidays 2012, we introduced a 3-part print that we could produce quickly at Costco rather than blocking on a
 * printing partner. This printable image is a standalone certificate of authenticity.
 *
 * See SER-535 for more information.
 */
object StandaloneCertificatePrint {
  // This number must version any time there is a breaking change to the images.
  val currentVersion = 2
}

case class StandaloneCertificatePrint() extends HasCertOfAuthenticity {

  // Definitions for HasCertOfAuthenticity
  protected def certW: Int = 924
  protected def certH: Int = 1294
  protected def certBannerWidth: Int = 114
  protected def targetLogoWidth: Int = 60
  protected def celebFullNameYOffset: Int = 375
  protected def recipientNameYOffset: Int = 535
  protected def createdByTextYOffset: Int = 300
  protected def forTextYOffset: Int = 450
  protected def dateYOffset: Int = 600
  protected def qrCodeYOffset: Int = 630
  protected def egraphNumberYOffset: Int = 875
  protected def imageNameYOffset: Int = 935
  protected def instructionTextYOffset: Int = 1050
  protected def logoYOffset: Int = 1200
  protected def certBackground: BufferedImage = ImageIO.read(current.resourceAsStream("images/cert-5x7.png").get)

  def assemble(orderNumber: String,
               teamLogoImage: BufferedImage,
               recipientName: String,
               celebFullName: String,
               celebCasualName: String,
               productName: String,
               signedAtDate: Date,
               egraphUrl: String): BufferedImage = {

    val canvas = new BufferedImage(certW, certH, BufferedImage.TYPE_INT_RGB)
    val g2 = canvas.createGraphics
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    val data = CertificateData(orderNumber = orderNumber,
      teamLogoImage = teamLogoImage,
      recipientName = recipientName,
      celebFullName = celebFullName,
      celebCasualName = celebCasualName,
      productName = productName,
      signedAtDate = signedAtDate,
      egraphUrl = egraphUrl,
      certX = 0,
      certY = 0
    )
    drawCertificate(g2 = g2, data = data)

    //done with graphics as assembly is the final action
    g2.dispose()

    canvas
  }
}
