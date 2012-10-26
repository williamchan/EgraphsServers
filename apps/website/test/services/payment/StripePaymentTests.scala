package services.payment

import utils.EgraphsUnitTest
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StripePaymentTests extends EgraphsUnitTest
{
  import services.Finance.TypeConversions._

  private def payment = {
    val payment = AppConfig.instance[StripeTestPayment]
    
    payment.bootstrap()
    
    payment
  }
  
  private val amount: BigDecimal = 60

  "charge" should "successfully charge a token" in new EgraphsTestApplication {
    val charge = payment.charge(amount.toMoney(), payment.testToken().id,
      "services.payment..StripePaymentTests, \"charge should successfully charge a token\". Looks like it worked.")
    charge.id should not be (null)
  }

  "charge" should "successfully work if amount is in smaller than penny increments" in new EgraphsTestApplication {
    val charge = payment.charge(BigDecimal(10.12345).toMoney(), payment.testToken().id,
      "services.payment..StripePaymentTests, \"charge should successfully charge a token\". Looks like it worked.")
    charge.id should not be (null)
  }

  "charge" should "throw if double-charging a token" in new EgraphsTestApplication {
    val token = payment.testToken()
    val charge = payment.charge(amount.toMoney(), token.id,
      "services.payment..StripePaymentTests, \"Test double-charging with token\".")
    charge.id should not be (null)

    val exception = intercept[com.stripe.exception.InvalidRequestException] {
      payment.charge(amount.toMoney(), token.id,
        "services.payment..StripePaymentTests, \"Test double-charging with token\".")
      fail("Should have thrown exception")
    }
    exception.getLocalizedMessage should include ("You cannot use a stripe token more than once")
  }

  "refund" should "successfully refund a charge" in new EgraphsTestApplication {
    val charge = payment.charge(amount.toMoney(), payment.testToken().id,
      "services.payment..StripePaymentTests, \"This charge should be refunded\".")
    charge.refunded should be(false)

    val refundedCharge = payment.refund(charge.id)
    refundedCharge.id should be(charge.id)
    refundedCharge.refunded should be(true)
  }

  "refund" should "throw exception if charge does not exist" in new EgraphsTestApplication {
    val exception = intercept[com.stripe.exception.InvalidRequestException] {
      payment.refund("doesnotexist")
      fail("Should have thrown exception")
    }
    exception.getLocalizedMessage should include ("No such charge")
  }

  "refund" should "throw exception if called on a charge that has already been refunded" in new EgraphsTestApplication {
    val charge = payment.charge(amount.toMoney(), payment.testToken().id,
      "services.payment..StripePaymentTests, \"This charge should be refunded\".")
    charge.refunded should be(false)

    val refundedCharge = payment.refund(charge.id)
    refundedCharge.id should be(charge.id)
    refundedCharge.refunded should be(true)

    val exception = intercept[com.stripe.exception.InvalidRequestException] {
      payment.refund(charge.id)
      fail("Should have thrown exception")
    }
    exception.getLocalizedMessage should include ("has already been refunded")
  }

  "testToken" should "throw exception when called from live implementation" in new EgraphsTestApplication {
    intercept[UnsupportedOperationException] {AppConfig.instance[StripePayment].testToken()}
  }

  "isTest" should "be false for live implementation, true for others" in new EgraphsTestApplication {
    AppConfig.instance[StripePayment].isTest should be (false)
    AppConfig.instance[StripeTestPayment].isTest should be (true)
    AppConfig.instance[YesMaamPayment].isTest should be (true)
  }
}