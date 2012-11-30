package services.print

import java.awt.image.BufferedImage
import java.util.Date

/**
 * Data meant to be passed to HasCertificate.drawCertificate.
 *
 * @param orderNumber order Id
 * @param teamLogoImage logo to be drawn on certificate
 * @param recipientName recipient's name
 * @param celebFullName celebrity's full name
 * @param celebCasualName celebrity's casual name
 * @param productName product name
 * @param signedAtDate date egraph was signed
 * @param egraphUrl URL to egraph
 * @param certX x-coordinate to begin drawing the certificate
 * @param certY y-coordinate to begin drawing the certificate
 */
case class CertificateData(orderNumber: String,
                           teamLogoImage: BufferedImage,
                           recipientName: String,
                           celebFullName: String,
                           celebCasualName: String,
                           productName: String,
                           signedAtDate: Date,
                           egraphUrl: String,
                           certX: Int,
                           certY: Int)
