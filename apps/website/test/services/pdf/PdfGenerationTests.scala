package services.pdf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class PdfGenerationTests extends EgraphsUnitTest {

  "GiftCertificatePdf" should "generate a gift certificate pdf" in new EgraphsTestApplication {
    val result = GiftCertificatePdf().generate(recipientName = "Joshua Robinson has a really long name",
      buyerName = "Alexander Kehlenbeck",
      amount = 75,
      code = "12345678901234567890")
    result.toByteArray.length should be(232961)
  }

  "EgraphExplanationPdf" should "generate a egraph explanation pdf" in new EgraphsTestApplication {
    val result = EgraphExplanationPdf().generate(recipientName = "Joshua Robinson",
      buyerName = "Alexander Kehlenbeck",
      celebrityName = "Sergio Romo")
    result.toByteArray.length should be(868391)
  }

}
