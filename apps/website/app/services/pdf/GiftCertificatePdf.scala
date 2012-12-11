package services.pdf

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.{PDSimpleFont, PDType1Font}
import play.api.Play._

object GiftCertificatePdf {

  private val helvetica = PDType1Font.HELVETICA

  def execute(recipientName: String,
              buyerName: String,
              amount: Int,
              code: String) {
    /**
     * The bottom-left corner of a PDPageContentStream is (0,0) and the top-right corner is (8.5*dpi, 11*dpi).
     * The default dpi is 72, due to PDPage.
     */
    val document = PDDocument.load(current.resourceAsStream("documents/gift_certificate_template.pdf").get)
    val page = document.getDocumentCatalog.getAllPages.get(0).asInstanceOf[PDPage]
    implicit val contentStream = new PDPageContentStream(document, page, /*appendContent*/ true, /*compress*/ true)

    val pageWidth = PDPage.PAGE_SIZE_LETTER.getWidth.toInt
    val pageCenter = (pageWidth / 2)
    val pageHeight = PDPage.PAGE_SIZE_LETTER.getHeight.toInt

    drawTextCentered(helvetica, 30, recipientName.toUpperCase, y = pageHeight - 210, xCenter = pageCenter)
    drawTextCentered(helvetica, 18, buyerName.toUpperCase, y = pageHeight - 300, xCenter = 155)
    drawTextCentered(helvetica, 35, "$" + amount, y = pageHeight - 305, xCenter = pageCenter)
    drawTextCentered(helvetica, 18, code, y = pageHeight - 300, xCenter = 457)

    contentStream.close()
    document.save("tmp/files/test.pdf")
    document.close()
  }

  /**
   * Draws the text horizontally centered along some xCenter.
   */
  private def drawTextCentered(font: PDSimpleFont,
                               fontSize: Int,
                               text: String,
                               y: Int,
                               xCenter: Int)(implicit contentStream: PDPageContentStream) {
    val textWidth = font.getStringWidth(text) / 1000 * fontSize
    contentStream.beginText()
    contentStream.setNonStrokingColor(100, 100, 100)
    contentStream.setFont(font, fontSize)
    contentStream.moveTextPositionByAmount(xCenter - textWidth / 2, y)
    contentStream.drawString(text)
    contentStream.endText()
  }
}
