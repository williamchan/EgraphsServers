package services.payment

import utils.EgraphsUnitTest
import services.inject.ClosureProviders
import services.{AppConfig, Utils}

class PaymentModuleTest extends EgraphsUnitTest with ClosureProviders {

  "PaymentProvider" should "return the stripe implementation when 'stripe' is set in application.conf" in {
    val (underTest, _, _, _, utils) = paymentProviderAndDeps

    utils.requiredConfigurationProperty(any) returns "stripe"
    underTest.get().isInstanceOf[StripePayment] should be (true)
  }

  "PaymentProvider" should "return the stripetest implementation when 'stripetest' is set in application.conf" in {
    val (underTest, _, _, _, utils) = paymentProviderAndDeps

    utils.requiredConfigurationProperty(any) returns "stripetest"
    underTest.get().isInstanceOf[StripeTestPayment] should be (true)
  }

  it should "return the yes maam implementation when 'yesmaam' is set in application.conf" in {
    val (underTest, _, _, _, utils) = paymentProviderAndDeps

    utils.requiredConfigurationProperty(any) returns "yesmaam"
    underTest.get().isInstanceOf[YesMaamPayment] should be (true)
  }

  it should "throw an IllegalArgumentException when application.conf sets an unrecognizable setting" in {
    val (underTest, _, _, _, utils) = paymentProviderAndDeps

    utils.requiredConfigurationProperty(any) returns "herp"
    evaluating {
      underTest.get()
    } should produce[IllegalArgumentException]
  }

  "AppConfig" should "be able to give us a Payment implementation" in {
    // If this throws an exception then it broke.
    AppConfig.instance[Payment]
  }

  def paymentProviderAndDeps = {
    val stripeImpl = mock[StripePayment]
    val stripeTestImpl = mock[StripeTestPayment]
    val yesMaamImpl = mock[YesMaamPayment]
    val utils = mock[Utils]

    (new PaymentProvider(stripeImpl, stripeTestImpl, yesMaamImpl, utils),
      stripeImpl,
      stripeTestImpl,
      yesMaamImpl,
      utils
      )
  }
}
