package services.payment

import utils.EgraphsUnitTest
import services.inject.ClosureProviders
import services.{AppConfig, Utils}
import services.config.ConfigFileProxy

class PaymentModuleTest extends EgraphsUnitTest with ClosureProviders {

  "PaymentProvider" should "return the stripe implementation when 'stripe' is set in application.conf" in {
    val (underTest, _, _, _, config) = paymentProviderAndDeps

    config.paymentVendor returns "stripe"
    underTest.get().isInstanceOf[StripePayment] should be (true)
  }

  it should "return the stripetest implementation when 'stripetest' is set in application.conf" in {
    val (underTest, _, _, _, config) = paymentProviderAndDeps

    config.paymentVendor returns "stripetest"
    underTest.get().isInstanceOf[StripeTestPayment] should be (true)
  }

  it should "return the yes maam implementation when 'yesmaam' is set in application.conf" in {
    val (underTest, _, _, _, config) = paymentProviderAndDeps

    config.paymentVendor returns "yesmaam"
    underTest.get().isInstanceOf[YesMaamPayment] should be (true)
  }

  it should "throw an IllegalArgumentException when application.conf sets an unrecognizable setting" in {
    val (underTest, _, _, _, config) = paymentProviderAndDeps

    config.paymentVendor returns "herp"
    evaluating {
      underTest.get()
    } should produce[IllegalArgumentException]
  }

  "AppConfig" should "be able to give us a Payment implementation" in new TestApplication {
    // If this throws an exception then it broke.
    AppConfig.instance[Payment] should not be (null)
  }

  private def paymentProviderAndDeps = {
    val stripeImpl = mock[StripePayment]
    val stripeTestImpl = mock[StripeTestPayment]
    val yesMaamImpl = mock[YesMaamPayment]
    val configFileProxy = mock[ConfigFileProxy]

    (new PaymentProvider(stripeImpl, stripeTestImpl, yesMaamImpl, configFileProxy),
      stripeImpl,
      stripeTestImpl,
      yesMaamImpl,
      configFileProxy
    )
  }
}
