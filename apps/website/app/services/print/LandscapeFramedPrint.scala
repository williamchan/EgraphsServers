package services.print

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.Date
import java.awt.RenderingHints
import play.api.Play.current
import services.ImageUtil

object LandscapeFramedPrint {
  // This number must version any time there is a breaking change to the images.
  val currentVersion = 3

  val targetEgraphWidth = 2150
}

case class LandscapeFramedPrint() extends HasCertOfAuthenticity {

  val width = 3600
  val height = 2400
  val sideMargin = 188
  val egraphX = sideMargin
  val widthBetweenEgraphAndCert = 150

  // Definitions for HasCertOfAuthenticity
  protected def certW: Int = 924
  protected def certH: Int = 1361
  protected def certBannerWidth: Int = 114
  protected def targetLogoWidth: Int = 60
  protected def celebFullNameYOffset: Int = 375
  protected def recipientNameYOffset: Int = 535
  protected def createdByTextYOffset: Int = 300
  protected def forTextYOffset: Int = 450
  protected def dateYOffset: Int = 625
  protected def qrCodeYOffset: Int = 680
  protected def egraphNumberYOffset: Int = 950
  protected def imageNameYOffset: Int = 1025
  protected def instructionTextYOffset: Int = 1125
  protected def logoYOffset: Int = 1267
  protected def certBackground: BufferedImage = ImageIO.read(current.resourceAsStream("images/landscape-framed-print-cert.png").get)

  def assemble(orderNumber: String,
               egraphImage: BufferedImage,
               teamLogoImage: BufferedImage,
               recipientName: String,
               celebFullName: String,
               celebCasualName: String,
               productName: String,
               signedAtDate: Date,
               egraphUrl: String): BufferedImage = {

    val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g2 = canvas.createGraphics
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    val scaledEgraphImage = ImageUtil.getScaledImage(img = egraphImage, targetWidth = LandscapeFramedPrint.targetEgraphWidth)
    val egraphY = (height - scaledEgraphImage.getHeight) / 2
    g2.drawImage(scaledEgraphImage, egraphX, egraphY, null)

    val data = CertificateData(orderNumber = orderNumber,
      teamLogoImage = teamLogoImage,
      recipientName = recipientName,
      celebFullName = celebFullName,
      celebCasualName = celebCasualName,
      productName = productName,
      signedAtDate = signedAtDate,
      egraphUrl = egraphUrl,
      certX = egraphX + LandscapeFramedPrint.targetEgraphWidth + widthBetweenEgraphAndCert,
      certY = egraphY
    )
    drawCertificate(g2 = g2, data = data)

    //done with graphics as assembly is the final action
    g2.dispose()

    canvas
  }
}
