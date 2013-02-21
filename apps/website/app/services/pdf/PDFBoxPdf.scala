package services.pdf

import scala.language.postfixOps
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.PDPage

/**
 * Provides helpful utilities for use with the Apache PDFBox library.
 */
trait PDFBoxPdf {

  /**
   * The bottom-left corner of a PDPageContentStream is (0,0) and the top-right corner is (8.5*dpi, 11*dpi).
   * The default dpi is 72, due to PDPage.
   */
  protected val pageWidth = PDPage.PAGE_SIZE_LETTER.getWidth.toInt
  protected val pageCenter = (pageWidth / 2)
  protected val pageHeight = PDPage.PAGE_SIZE_LETTER.getHeight.toInt
  private val minFontSize = 4

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
  protected def drawTextCentered(font: PDFont,
                                 maxFontSize: Int,
                                 text: String,
                                 y: Int,
                                 xCenter: Int = pageCenter,
                                 radius: Int = pageCenter - 80)(implicit contentStream: PDPageContentStream) {

    // Recursively down-size font until the text fits within the specified radius.
    val fontSize = (minFontSize to maxFontSize reverse).find(s => textWidth(font, s, text) <= (radius * 2)).getOrElse(minFontSize)
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
  protected def textWidth(font: PDFont, fontSize: Int, text: String): Float = {
    font.getStringWidth(text) / 1000 * fontSize
  }

  /**
   * @return height of text at this font and font size
   */
  protected def textHeight(font: PDFont, fontSize: Int): Float = {
    font.getFontDescriptor.getFontBoundingBox.getHeight / 1000 * fontSize
  }
}
