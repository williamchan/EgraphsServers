package services.payment

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import org.scalatest.BeforeAndAfterEach
import utils.{TestData, DBTransactionPerTest, ClearsDatabaseAndValidationAfter}
import services.AppConfig

class StripePaymentTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  import services.Finance.TypeConversions._

  val payment = AppConfig.instance[StripePayment]
  val amount: BigDecimal = 1000

  payment.bootstrap()
  "charge" should "successfully charge a token" in {
    val token = payment.testToken

    val charge = payment.charge(
      amount.toMoney(),
      token.id,
      "services.payment..StripePaymentTests, \"charge should successfully charge a token\". Looks like it worked."
    )

    charge.id should not be (null)
  }
}