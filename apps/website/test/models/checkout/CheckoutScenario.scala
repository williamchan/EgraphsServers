package models.checkout

import models.checkout.Conversions._
import scala.Some
import models.{Address, Account}
import utils.TestData

/** container class for test data; also simplifies the signature of test case bodies used with `eachScenario` */
case class CheckoutScenario(
  initialTypes: LineItemTypes,
  updateWithTypes: LineItemTypes = Nil  ,
  buyer: Option[Account] = Some(TestData.newSavedAccount()),
  address: Option[String] = Some(TestData.random.nextString(10)),
  cashTransaction: Option[CashTransactionLineItemType] = Some(LineItemTestData.randomCashTransactionType)
) {
  import CheckoutScenario._
  import RichCheckoutConversions._


  /**
   * For running a test case on a single body, can be applied directly to scenario
   *
   * {{{
   *   "A FooLineItem" should "do stuff" in someCoolScenario { transactedCheckout should be ('cool) }
   * }}}
   */
  def apply(body: Body) = body(this)

  lazy val transactedCheckout = initialCheckout.transacted(this)
  lazy val restoredCheckout = transactedCheckout.restored(this)
  lazy val initialCheckout = {
    val checkout = Checkout.create(initialTypes).withShippingAddress(address)
    buyer map { checkout.withBuyerAccount(_) } getOrElse checkout
  }
}

object CheckoutScenario {
  type Predicate = CheckoutScenario => Boolean
  type Body = CheckoutScenario => Unit

  object RichCheckoutConversions {
    /** these make it simple to update, restore and transact checkouts in the context of a CheckoutScenario */
    implicit def checkoutHelperDsl(checkout: Checkout): RichCheckout = new RichCheckout(checkout)

    /** these make checkout data instamagically available in each test case */
    def initialCheckout(implicit scenario: CheckoutScenario) = scenario.initialCheckout
    def transactedCheckout(implicit scenario: CheckoutScenario) = scenario.transactedCheckout
    def restoredCheckout(implicit scenario: CheckoutScenario) = scenario.restoredCheckout

    protected class RichCheckout(checkout: Checkout) {
      def updated(implicit scenario: CheckoutScenario) = checkout.withAdditionalTypes(scenario.updateWithTypes)

      def restored(implicit scenario: CheckoutScenario) = {
        val toRestore = if (checkout.id <= 0) transacted else checkout
        Checkout.restore(toRestore.id).get
      }

      def transacted(implicit scenario: CheckoutScenario) = {
        import org.scalatest.Assertions.fail

        checkout.transact(scenario.cashTransaction) match {
          case Right(savedCheckout) => savedCheckout
          case Left(left) => left match {
            case Checkout.CheckoutFailedError(_, _, _, e) => fail("failed with exception: " + e)
            case failure => fail("failed with: " + failure)
          }
        }
      }
    }
  }

  object ScenarioPredicates {
    def compose(predicates: Predicate*): Predicate = { scenario => predicates forall (_.apply(scenario)) }

    def buyerDefined: Predicate = { scenario => scenario.buyer.isDefined }
    def updateDefined: Predicate = { scenario => scenario.updateWithTypes.nonEmpty }
    def paymentDefined: Predicate = { scenario => scenario.cashTransaction.isDefined }
    def paymentRequired: Predicate = { scenario => scenario.initialCheckout.balance.amount.isZero }

    def buyerAndPaymentDefined = compose(buyerDefined, paymentDefined)
  }
}