package services.pdf

import java.io.ByteArrayOutputStream
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import play.api.Play._

object EgraphExplanationPdf extends PDFBoxPdf {

  private val helvetica = PDType1Font.HELVETICA
  private val helveticaBold = PDType1Font.HELVETICA_BOLD

  /**
   * Generates an egraph explanation pdf.
   */
  def generate(recipientName: String,
               buyerName: String,
               celebrityName: String): ByteArrayOutputStream = {

    val document = PDDocument.load(current.resourceAsStream("documents/egraph_explanation_template.pdf").get)

    try {
      val page = document.getDocumentCatalog.getAllPages.get(0).asInstanceOf[PDPage]
      implicit val contentStream = new PDPageContentStream(document, page, /*appendContent*/ true, /*compress*/ true)

      drawTextCentered(helvetica, 30, celebrityName.toUpperCase, y = pageHeight - 170)
      drawTextCentered(helveticaBold, 35, recipientName.toUpperCase, y = pageHeight - 238)

      drawTextCentered(helveticaBold, 13, "You're the recipient of an egraph from " + celebrityName + ".", y = pageHeight - 297)
      drawTextCentered(helvetica, 13, "He will be writing a note and creating a personal voice message", y = pageHeight - 332)
      drawTextCentered(helvetica, 13, "just for you. Learn more at egraphs.com", y = pageHeight - 350)

      drawTextCentered(helvetica, 20, buyerName.toUpperCase, y = pageHeight - 655)

      drawTextCentered(helvetica, 13, "You will receive an email when your egraph is ready!", y = pageHeight - 700)

      contentStream.close()

//      document.save("tmp/files/test.pdf") // This call is useful during development to check the generated pdf.
      val output = new ByteArrayOutputStream()
      document.save(output)
      output

    } finally {
      document.close()
    }
  }
}
