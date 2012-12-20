package services.pdf

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.{PDFont, PDType1Font}
import play.api.Play._
import java.io.ByteArrayOutputStream

object GiftCertificatePdf {

  private val helvetica = PDType1Font.HELVETICA

  /**
   * Generates a gift certificate pdf.
   *
   * @param recipientName recipient's name
   * @param buyerName buyer's name. Would be great if we could limit this to 20 characters.
   * @param amount dollar amount of gift certificate.
   * @param code gift certificate redemption code. Would be great if we could limit this to 20 characters.
   */
  def generate(recipientName: String,
               buyerName: String,
               amount: Int,
               code: String): ByteArrayOutputStream = {
    /**
     * The bottom-left corner of a PDPageContentStream is (0,0) and the top-right corner is (8.5*dpi, 11*dpi).
     * The default dpi is 72, due to PDPage.
     */
    val document = PDDocument.load(current.resourceAsStream("documents/gift_certificate_template.pdf").get)
    try {
      val page = document.getDocumentCatalog.getAllPages.get(0).asInstanceOf[PDPage]
      implicit val contentStream = new PDPageContentStream(document, page, /*appendContent*/ true, /*compress*/ true)

      val pageWidth = PDPage.PAGE_SIZE_LETTER.getWidth.toInt
      val pageCenter = (pageWidth / 2)
      val pageHeight = PDPage.PAGE_SIZE_LETTER.getHeight.toInt

      /**
       * To pick the numbers used in the following calls, I opened the pdf template in Gimp at 72 dpi and moused over
       * text locations to get approximate x and y locations.
       */
      drawTextCentered(helvetica, 50, recipientName.toUpperCase, y = pageHeight - 210, xCenter = pageCenter, radius = pageCenter - 80)
      drawTextCentered(helvetica, 18, buyerName.toUpperCase, y = pageHeight - 300, xCenter = 155, radius = 155 - 85)
      drawTextCentered(helvetica, 35, "$" + amount, y = pageHeight - 305, xCenter = pageCenter, radius = pageCenter - 235)
      drawTextCentered(helvetica, 18, code, y = pageHeight - 300, xCenter = 457, radius = 155 - 85)

      contentStream.close()

      //    document.save("tmp/files/test.pdf") // This call is useful during development to check the generated pdf.
      val output = new ByteArrayOutputStream()
      document.save(output)
      output

    } finally {
      document.close()
    }
  }

  /**
   * /**
   * Draws the text horizontally centered along some xCenter.
   */
   * @param font the font to use
   * @param maxFontSize the maximum font size to use. The actual font size used might be smaller.
   * @param text text to draw
   * @param y y location. The text is drawn above this y location.
   * @param xCenter x location around which to center the center
   * @param radius the farther the text can extend on either side of xCenter. The font will be decreased until text fits within this radius.
   */
  private def drawTextCentered(font: PDFont,
                               maxFontSize: Int,
                               text: String,
                               y: Int,
                               xCenter: Int,
                               radius: Int)(implicit contentStream: PDPageContentStream) {

    // Recursively down-size font until the text fits within the specified radius.
    val fontSize = (1 to maxFontSize reverse).find(s => textWidth(font, s, text) <= (radius * 2)).getOrElse(8)
    // The text y-location should be moved up if the font was down-sized. Why quotient of "4"? Because it just seems to work.
    val actualY = y + (textHeight(font, maxFontSize - fontSize) / 4)

    contentStream.beginText()
    contentStream.setNonStrokingColor(100, 100, 100)
    contentStream.setFont(font, fontSize)
    contentStream.moveTextPositionByAmount(xCenter - textWidth(font, fontSize, text) / 2, actualY)
    contentStream.drawString(text)
    contentStream.endText()
  }

  /**
   * @return width of text at this font and font size
   */
  private def textWidth(font: PDFont, fontSize: Int, text: String): Float = {
    font.getStringWidth(text) / 1000 * fontSize
  }

  /**
   * @return height of text at this font and font size
   */
  private def textHeight(font: PDFont, fontSize: Int): Float = {
    font.getFontDescriptor.getFontBoundingBox.getHeight / 1000 * fontSize
  }
}
