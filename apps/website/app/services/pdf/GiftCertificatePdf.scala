package services.pdf

import java.io.ByteArrayOutputStream
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import play.api.Play._

object GiftCertificatePdf extends PDFBoxPdf {

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

    val document = PDDocument.load(current.resourceAsStream("documents/gift_certificate_template.pdf").get)
    try {
      val page = document.getDocumentCatalog.getAllPages.get(0).asInstanceOf[PDPage]
      implicit val contentStream = new PDPageContentStream(document, page, /*appendContent*/ true, /*compress*/ true)

      /**
       * To pick the numbers used in the following calls, I opened the pdf template in Gimp at 72 dpi and moused over
       * text locations to get approximate x and y locations.
       */
      drawTextCentered(helvetica, 50, recipientName.toUpperCase, y = pageHeight - 210)
      drawTextCentered(helvetica, 18, buyerName.toUpperCase, y = pageHeight - 300, xCenter = 155, radius = 155 - 85)
      drawTextCentered(helvetica, 35, "$" + amount, y = pageHeight - 305)
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
}
