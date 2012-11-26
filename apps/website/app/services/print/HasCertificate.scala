package services.print

import java.awt.image.BufferedImage
import java.awt.Graphics2D

trait HasCertificate {

  def drawCertificate(canvas: BufferedImage, g2: Graphics2D, data: CertificateData)

}
