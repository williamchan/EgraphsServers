package services.payment

import utils.EgraphsUnitTest
import services.Finance

class YesMaamPaymentTests extends EgraphsUnitTest {
  import Finance.TypeConversions._

  val payment = new YesMaamPayment

  "YesMaamPayment" should "produce a token and accept its charge" in {
    payment.bootstrap()
    val token = payment.testToken()

    payment.charge(
      new BigDecimal(new java.math.BigDecimal(100)).toMoney(),
      token.id,
      "This is the description, maam"
    )

    // If you're reading this I guess these commands failed to compile or threw an
    // exception. That's the only reason this is here. Just wanted to say hi.
  }
}

