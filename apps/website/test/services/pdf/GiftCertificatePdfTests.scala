package services.pdf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class GiftCertificatePdfTests extends EgraphsUnitTest {

    "it" should "run" in new EgraphsTestApplication {
      GiftCertificatePdf.execute(recipientName = "Joshua Robinson",
        buyerName = "Jason Shaw",
        amount = 75,
        code = "123456789012")
    }

}
