package services.print

import java.awt.{Color, Font, Graphics2D}
import java.awt.image.BufferedImage
import java.awt.geom.Rectangle2D
import services.ImageUtil

trait HasCertOfAuthenticity {

  protected def certW: Int
  protected def certH: Int
  protected def certBannerWidth: Int
  protected def targetLogoWidth: Int

  protected def celebFullNameYOffset: Int
  protected def recipientNameYOffset: Int
  protected def createdByTextYOffset: Int
  protected def forTextYOffset: Int
  protected def dateYOffset: Int
  protected def qrCodeYOffset: Int
  protected def egraphNumberYOffset: Int
  protected def imageNameYOffset: Int
  protected def instructionTextYOffset: Int
  protected def logoYOffset: Int
  protected def certBackground: BufferedImage

  /**
   * Draws a certificated onto the g2 Graphics2D instance according to the data param.
   *
   * @param g2 an instance of Graphics2D on which the certificate images and text are drawn
   * @param data specifies what dynamic images and text to draw on the certificate of authenticity
   */
  protected def drawCertificate(g2: Graphics2D, data: CertificateData) {
    var x: Int = 0
    var y: Int = 0
    val certX = data.certX
    val certY = data.certY
    val certWritableX = certX + certBannerWidth
    val certWritableWidth = certW - certBannerWidth

    val celebFullNameY = certY + celebFullNameYOffset
    val recipientNameY = certY + recipientNameYOffset
    val createdByTextY = certY + createdByTextYOffset
    val forTextY = certY + forTextYOffset
    val dateY = certY + dateYOffset
    val qrCodeY = certY + qrCodeYOffset
    val egraphNumberY = certY + egraphNumberYOffset
    val imageNameY = certY + imageNameYOffset
    val instructionTextY = certY + instructionTextYOffset
    val logoY = certY + logoYOffset

    var fontRec: Rectangle2D = null
    var centuryGothicFont = new Font("Century Gothic", Font.PLAIN, 55)
    var calistoMTFont = new Font("Calisto MT", Font.ITALIC, 50)
    val scaledLogoImage = ImageUtil.getScaledImage(img = data.teamLogoImage, targetWidth = targetLogoWidth)

    //add certificate backdrop image - currently just a white rectangle
    g2.setColor(Color.WHITE)
    g2.fillRect(certX, certY, certW, certH)
    g2.drawImage(certBackground, certX, certY, null)

    //draw CelebFullName
    val celebFullNameCaps = data.celebFullName.toUpperCase
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
    y = celebFullNameY
    g2.drawString(celebFullNameCaps, x, y)

    //draw recipientName
    val recipientNameCaps = data.recipientName.toUpperCase
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
    y = recipientNameY
    g2.drawString(recipientNameCaps, x, y)

    //draw this egraph created by
    g2.setFont(calistoMTFont)
    g2.setColor(Color.GRAY)
    frc = g2.getFontRenderContext
    fontRec = calistoMTFont.getStringBounds("this egraph created by:", frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = createdByTextY
    g2.drawString("this egraph created by:", x, y)
    //for:
    fontRec = calistoMTFont.getStringBounds("for:", frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = forTextY
    g2.drawString("for:", x, y)

    //draw date
    calistoMTFont = calistoMTFont.deriveFont(Font.ITALIC, 45.toFloat)
    g2.setFont(calistoMTFont)
    fontRec = calistoMTFont.getStringBounds(data.signedAtDate.toString, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = dateY
    g2.drawString(data.signedAtDate.toString, x, y)

    //add QR code
    val qr = CreateQR.generate(url = data.egraphUrl)
    val qrX = certWritableX + ((certWritableWidth - qr.getWidth) / 2)
    g2.drawImage(qr, qrX, qrCodeY, null)

    //draw Egraph#
    g2.setColor(Color.BLACK)
    calistoMTFont = calistoMTFont.deriveFont(Font.PLAIN, 35.toFloat)
    g2.setFont(calistoMTFont)
    frc = g2.getFontRenderContext
    fontRec = calistoMTFont.getStringBounds("Egraph# " + data.orderNumber, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = egraphNumberY
    g2.drawString("Egraph# " + data.orderNumber, x, y)

    //draw image name
    val productNameLength = data.productName.length
    val productNameLines = if (productNameLength < 50) {
      List(data.productName)
    } else {
      val breakCharIndex = data.productName.substring(productNameLength / 2).indexWhere(_ == ' ') + (productNameLength / 2)
      val s = data.productName.splitAt(breakCharIndex + 1)
      List(s._1.trim, s._2.trim)
    }
    fontRec = calistoMTFont.getStringBounds(productNameLines.head, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = imageNameY
    g2.drawString(productNameLines.head, x, y)
    if (productNameLines.tail.headOption.isDefined) {
      val productNameLine2 = productNameLines.tail.head
      fontRec = calistoMTFont.getStringBounds(productNameLine2, frc)
      x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
      y = imageNameY + (fontRec.getHeight).toInt
      g2.drawString(productNameLine2, x, y)
    }

    //draw snap the QR code for additional authentication information and a special audio message from
    var snap = "Snap the QR code for a personalized audio"
    calistoMTFont = calistoMTFont.deriveFont(Font.PLAIN, 35.toFloat)
    g2.setFont(calistoMTFont)
    fontRec = calistoMTFont.getStringBounds(snap, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = instructionTextY
    g2.drawString(snap, x, y)
    snap = "message from "
    y = instructionTextY + fontRec.getHeight.toInt //add previous height before resetting
    fontRec = calistoMTFont.getStringBounds(snap + data.celebCasualName, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    g2.drawString(snap + data.celebCasualName, x, y)

    //draw www.egraphs.com/
    y = instructionTextY + (2 * fontRec.getHeight).toInt //add previous height before resetting
    fontRec = calistoMTFont.getStringBounds("www.egraphs.com/" + data.orderNumber, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("www.egraphs.com/" + data.orderNumber, x, y)

    //draw MLB logos
    x = certWritableX + ((certWritableWidth - targetLogoWidth) / 2)
    g2.drawImage(scaledLogoImage, x, logoY, null)
  }
}
