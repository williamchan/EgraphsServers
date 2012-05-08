package services.payment

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import org.scalatest.BeforeAndAfterEach
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter}
import services.AppConfig

class StripePaymentTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  import services.Finance.TypeConversions._

  private val payment = AppConfig.instance[StripePayment]
  private val amount: BigDecimal = 60

  payment.bootstrap()

  "charge" should "successfully charge a token" in {
    val token = payment.testToken()
    val charge = payment.charge(amount.toMoney(), token.id,
      "services.payment..StripePaymentTests, \"charge should successfully charge a token\". Looks like it worked.")
    charge.id should not be (null)
  }

  "charge" should "throw if double-charging a token" in {
    val token = payment.testToken()
    val charge = payment.charge(amount.toMoney(), token.id,
      "services.payment..StripePaymentTests, \"Test double-charging with token\".")
    charge.id should not be (null)

    val exception = intercept[com.stripe.exception.InvalidRequestException] {
      payment.charge(amount.toMoney(), token.id,
        "services.payment..StripePaymentTests, \"Test double-charging with token\".")
      fail("Should have thrown exception")
    }
    exception.getLocalizedMessage.contains("You cannot use a stripe token more than once") should be(true)
  }

  "refund" should "successfully refund a charge" in {
    val token = payment.testToken()
    val charge = payment.charge(amount.toMoney(), token.id,
      "services.payment..StripePaymentTests, \"This charge should be refunded\".")
    charge.refunded should be(false)

    val refundedCharge = payment.refund(charge.id)
    refundedCharge.id should be(charge.id)
    refundedCharge.refunded should be(true)
  }

  "refund" should "throw exception if charge does not exist" in {
    val exception = intercept[com.stripe.exception.InvalidRequestException] {
      payment.refund("doesnotexist")
      fail("Should have thrown exception")
    }
    exception.getLocalizedMessage.contains("No such charge") should be(true)
  }

  "refund" should "throw exception if called on a charge that has already been refunded" in {
    val token = payment.testToken()
    val charge = payment.charge(amount.toMoney(), token.id,
      "services.payment..StripePaymentTests, \"This charge should be refunded\".")
    charge.refunded should be(false)

    val refundedCharge = payment.refund(charge.id)
    refundedCharge.id should be(charge.id)
    refundedCharge.refunded should be(true)

    val exception = intercept[com.stripe.exception.InvalidRequestException] {
      payment.refund(charge.id)
      fail("Should have thrown exception")
    }
    exception.getLocalizedMessage.contains("has already been refunded") should be(true)
  }
}