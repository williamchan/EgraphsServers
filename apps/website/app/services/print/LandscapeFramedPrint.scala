package services.print

import javax.imageio.ImageIO
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.util.Date
import java.awt.{Color, Font, Graphics2D, RenderingHints}
import play.api.Play.current
import services.ImageUtil

object LandscapeFramedPrint {
  // This number must version any time there is a breaking change to the images.
  val currentVersion = 2

  val targetEgraphWidth = 2150
}

case class LandscapeFramedPrint() extends HasCertificate {

  val width = 3600
  val height = 2400

  val targetLogoWidth = 60

  val sideMargin = 188
  val egraphX = sideMargin
  val widthBetweenEgraphAndCert = 150
  val certBannerWidth = 114
  val certW = 924
  val certH = 1361

  private def certBackground: BufferedImage = ImageIO.read(current.resourceAsStream("images/landscape-framed-print-cert.png").get)

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
      certY = egraphY)
    drawCertificate(canvas = canvas, g2 = g2, data = data)

    //done with graphics as assembly is the final action
    g2.dispose()

    canvas
  }

  def drawCertificate(canvas: BufferedImage, g2: Graphics2D, data: CertificateData) {
    var x: Int = 0
    var y: Int = 0
    val certX = data.certX
    val certY = data.certY
    val certWritableX = certX + certBannerWidth
    val certWritableWidth = certW - certBannerWidth

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
    y = certY + 375
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
    y = certY + 535
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
    y = certY + 450
    g2.drawString("for:", x, y)

    //draw date
    calistoMTFont = calistoMTFont.deriveFont(Font.ITALIC, 45.toFloat)
    g2.setFont(calistoMTFont)
    fontRec = calistoMTFont.getStringBounds(data.signedAtDate.toString, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 625
    g2.drawString(data.signedAtDate.toString, x, y)

    //add QR code
    val qr = CreateQR.generate(url = data.egraphUrl)
    val qrX = certWritableX + ((certWritableWidth - qr.getWidth) / 2)
    g2.drawImage(qr, qrX, 1200, null)

    //draw Egraph#
    g2.setColor(Color.BLACK)
    calistoMTFont = calistoMTFont.deriveFont(Font.PLAIN, 35.toFloat)
    g2.setFont(calistoMTFont)
    frc = g2.getFontRenderContext
    fontRec = calistoMTFont.getStringBounds("Egraph# " + data.orderNumber, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    y = certY + 950
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
    y = certY + 1025
    g2.drawString(productNameLines.head, x, y)
    if (productNameLines.tail.headOption.isDefined) {
      val productNameLine2 = productNameLines.tail.head
      fontRec = calistoMTFont.getStringBounds(productNameLine2, frc)
      x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
      y = certY + 1025 + (fontRec.getHeight).toInt
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
    fontRec = calistoMTFont.getStringBounds(snap + data.celebCasualName, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    g2.drawString(snap + data.celebCasualName, x, y)

    //draw www.egraphs.com/
    y = certY + 1125 + (2 * fontRec.getHeight).toInt //add previous height before resetting
    fontRec = calistoMTFont.getStringBounds("www.egraphs.com/" + data.orderNumber, frc)
    x = certWritableX + ((certWritableWidth - fontRec.getWidth) / 2).toInt
    g2.drawString("www.egraphs.com/" + data.orderNumber, x, y)

    //draw MLB logos
    g2.drawImage(scaledLogoImage, certWritableX + 475, certY + 1267, null)
  }
}
