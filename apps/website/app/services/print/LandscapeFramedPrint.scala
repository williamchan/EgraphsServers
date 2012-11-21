package services.print

import javax.imageio.ImageIO
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.util.Date
import java.awt.{Font, Color, RenderingHints}
import play.api.Play.current

object LandscapeFramedPrint {

  val targetEgraphWidth = 2150
  val targetLogoWidth = 60

  // This number must version any time there is a breaking change to the images.
  val currentVersion = 2
}

case class LandscapeFramedPrint() {

  val width = 3600
  val height = 2400
  val sideMargin = 188
  val egraphX = sideMargin

  val widthBetweenEgraphAndCert = 150
  val certW = 924
  val certBannerWidth = 114
  val certX = 3600 - sideMargin - certW
  val certWritableX = certX + certBannerWidth
  val certWritableWidth = certW - certBannerWidth

  def assemble(orderNumber: String,
               egraphImage: BufferedImage,
               teamLogoImage: BufferedImage,
               recipientName: String,
               celebFullName: String,
               celebCasualName: String,
               productName: String,
               signedAtDate: Date,
               egraphUrl: String): BufferedImage = {

    val scaledEgraphImage = getScaledImage(image = egraphImage, targetWidth = LandscapeFramedPrint.targetEgraphWidth)
    val scaledLogoImage = getScaledImage(image = teamLogoImage, targetWidth = LandscapeFramedPrint.targetLogoWidth)

    val certH = scaledEgraphImage.getHeight
    val egraphY = (height - scaledEgraphImage.getHeight) / 2
    val certY = egraphY

    var x: Int = 0
    var y: Int = 0
    var fontRec: Rectangle2D = null
    var centuryGothicFont = new Font("Century Gothic", Font.PLAIN, 55)
    var calistoMTFont = new Font("Calisto MT", Font.ITALIC, 50)

    val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g2 = canvas.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    g2.drawImage(scaledEgraphImage, egraphX, egraphY, null)

    //add certificate backdrop image - currently just a white rectangle
    g2.setColor(Color.WHITE)
    g2.fillRect(certX, certY, certW, certH)
    g2.drawImage(certificateBackgroundImage, certX, certY, null)

    //draw CelebFullName
    val celebFullNameCaps = celebFullName.toUpperCase
    g2.setFont(centuryGothicFont)
    g2.setColor(Color.BLACK)
    var frc = g2.getFontRenderContext
    fontRec = centuryGothicFont.getStringBounds(celebFullNameCaps, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    while (x < certWritableX) {
      //adjust font size down for names that are too long
      centuryGothicFont = centuryGothicFont.deriveFont((centuryGothicFont.getSize - 5).toFloat)
      g2.setFont(centuryGothicFont)
      frc = g2.getFontRenderContext
      fontRec = centuryGothicFont.getStringBounds(celebFullNameCaps, frc)
      x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    }
    y = certY + 375
    g2.drawString(celebFullNameCaps, x, y)

    //draw recipientName
    val recipientNameCaps = recipientName.toUpperCase
    centuryGothicFont = centuryGothicFont.deriveFont(65.toFloat)
    g2.setFont(centuryGothicFont)
    frc = g2.getFontRenderContext
    fontRec = centuryGothicFont.getStringBounds(recipientNameCaps, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    while (x < certWritableX) {
      //adjust font size down for names that are too long
      centuryGothicFont = centuryGothicFont.deriveFont((centuryGothicFont.getSize - 5).toFloat)
      g2.setFont(centuryGothicFont)
      frc = g2.getFontRenderContext
      fontRec = centuryGothicFont.getStringBounds(recipientNameCaps, frc)
      x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    }
    y = certY + 350 + 185
    g2.drawString(recipientNameCaps, x, y)

    //draw this egraph created by
    g2.setFont(calistoMTFont)
    g2.setColor(Color.GRAY)
    frc = g2.getFontRenderContext
    fontRec = calistoMTFont.getStringBounds("this egraph created by:", frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 300
    g2.drawString("this egraph created by:", x, y)
    //for:
    fontRec = calistoMTFont.getStringBounds("for:", frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 300 + 150
    g2.drawString("for:", x, y)

    //draw date
    calistoMTFont = calistoMTFont.deriveFont(Font.ITALIC, 45.toFloat)
    g2.setFont(calistoMTFont)
    fontRec = calistoMTFont.getStringBounds(signedAtDate.toString, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 350 + 225 + 50
    g2.drawString(signedAtDate.toString, x, y)

    //add QR code
    val qr = CreateQR.generate(url = egraphUrl)
    val qrX = certWritableX + ((certWritableWidth - qr.getWidth) / 2)
    g2.drawImage(qr, qrX, 1200, null)

    //draw Egraph#
    g2.setColor(Color.BLACK)
    calistoMTFont = calistoMTFont.deriveFont(Font.PLAIN, 35.toFloat)
    g2.setFont(calistoMTFont)
    frc = g2.getFontRenderContext
    fontRec = calistoMTFont.getStringBounds("Egraph# " + orderNumber, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 950
    g2.drawString("Egraph# " + orderNumber, x, y)

    //draw image name
    val productNameLength = productName.length
    val productNameLines = if (productNameLength < 50) {
      List(productName)
    } else {
      val breakCharIndex = productName.substring(productNameLength / 2).indexWhere(_ == ' ') + (productNameLength / 2)
      val s = productName.splitAt(breakCharIndex + 1)
      List(s._1.trim, s._2.trim)
    }
    fontRec = calistoMTFont.getStringBounds(productNameLines.head, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 950 + 75
    g2.drawString(productNameLines.head, x, y)
    if (productNameLines.tail.headOption.isDefined) {
      val productNameLine2 = productNameLines.tail.head
      fontRec = calistoMTFont.getStringBounds(productNameLine2, frc)
      x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
      y = certY + 950 + 75 + (fontRec.getHeight).toInt
      g2.drawString(productNameLine2, x, y)
    }

    //draw snap the QR code for additional authentication information and a special audio message from
    var snap = "Snap the QR code for a personalized"
    calistoMTFont = calistoMTFont.deriveFont(Font.PLAIN, 35.toFloat)
    g2.setFont(calistoMTFont)
    fontRec = calistoMTFont.getStringBounds(snap, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 1125
    g2.drawString(snap, x, y)
    snap = "audio message from "
    y = certY + 1125 + fontRec.getHeight.toInt //add previous height before resetting
    fontRec = calistoMTFont.getStringBounds(snap + celebCasualName, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    g2.drawString(snap + celebCasualName, x, y)

    //draw www.egraphs.com/
    y = certY + 1125 + (2 * fontRec.getHeight).toInt //add previous height before resetting
    fontRec = calistoMTFont.getStringBounds("www.egraphs.com/" + orderNumber, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("www.egraphs.com/" + orderNumber, x, y)

    //draw MLB logos
    g2.drawImage(scaledLogoImage, certWritableX + 475, certY + 1267, null)

    //done with graphics as assembly is the final action
    g2.dispose()

    canvas
  }

  /**
   * @return image scaled to targetWidth. Can also upscale.
   */
  private def getScaledImage(image: BufferedImage, targetWidth: Int): BufferedImage = {
    if (targetWidth != image.getWidth) {
      val scale = targetWidth.toFloat / image.getWidth
      val newW = (scale * image.getWidth).toInt
      val newH = (scale * image.getHeight).toInt
      val scaledImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
      val g2ScaledImage = scaledImage.createGraphics()
      g2ScaledImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
      g2ScaledImage.drawImage(image, 0, 0, newW, newH, null)
      g2ScaledImage.dispose()
      scaledImage
    } else {
      image
    }
  }

  private def certificateBackgroundImage: BufferedImage = {
    ImageIO.read(current.resourceAsStream("images/landscape-framed-print-cert.png").get)
  }
}
