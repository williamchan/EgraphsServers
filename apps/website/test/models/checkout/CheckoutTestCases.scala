package models.checkout

import LineItemTestData._
import org.joda.money.Money
import org.scalatest.matchers.{ShouldMatchers, Matcher}
import services.Finance.TypeConversions._
import LineItemMatchers._
import models.checkout.checkout.Conversions._
import models.enums.LineItemNature._
import org.scalatest.FlatSpec


/**
 * A mixin to run a bunch of tests on different checkout contents so that new Checkout-related types have the
 * option to be tested within their own test suites simply by mixing in this trait and configuring a few
 * CheckoutScenarios.
 *
 * Tests that do not depend on the contents of the checkout should be put into `CheckoutTests` so they aren't run
 * repeatedly for no reason.
 */
trait CheckoutTestCases { this: FlatSpec with ShouldMatchers =>
  import CheckoutScenario._
  import RichCheckoutConversions._
  import ScenarioPredicates._


  def scenarios: Seq[CheckoutScenario]

  //
  // Test cases
  //
  "A checkout" should "have a balance of zero after transaction" in (eachScenario where buyerAndPaymentDefined) {
    implicit scenario =>

    val total = transactedCheckout.total
    val payment = transactedCheckout.payments.headOption
    val balance = transactedCheckout.balance

    balance should haveAmount (zeroDollars)
    total should haveAmount (payment map (_.amount.negated) getOrElse zeroDollars)
  }

  it should "charge the customer for the total on checkout" in (eachScenario where buyerAndPaymentDefined) {
    implicit scenario =>

    val total = initialCheckout.total
    val txnItem = transactedCheckout.payments.headOption

    total should haveAmount (txnItem map (_.amount.negated) getOrElse zeroDollars )
  }

  "A restored checkout" should "contain same line items as saved checkout" in (eachScenario where buyerAndPaymentDefined) {
    implicit scenario =>

    val savedItems = transactedCheckout.lineItems
    val restoredItems = restoredCheckout.lineItems

    savedItems.size should be (restoredItems.size)
    savedItems should haveLineItemEqualityTo (restoredItems)
  }

  it should "update when transacted with additional types" in (eachScenario where updateDefined and buyerAndPaymentDefined) {
    implicit scenario =>

    val updatedTransacted = transactedCheckout.updated.transacted
    val updatedRestored = updatedTransacted.restored

    updatedTransacted.id should be (transactedCheckout.id)
//    updatedTransacted._entity.created.getTime should be (transactedCheckout._entity.created.getTime)
    updatedTransacted._entity.updated.getTime should be > (transactedCheckout._entity.updated.getTime)
    updatedTransacted._entity.updated.getTime should be (updatedRestored._entity.updated.getTime)
  }


  it should "have correct balance" in (eachScenario where buyerAndPaymentDefined) { implicit scenario =>
    val updatedTransacted = restoredCheckout.updated.transacted
    val updatedRestored = updatedTransacted.restored

    restoredCheckout.balance should haveAmount (zeroDollars)
    updatedRestored.balance should haveAmount (zeroDollars)

    initialCheckout should haveExpectedBalance
    restoredCheckout should haveExpectedBalance
    updatedTransacted should haveExpectedBalance
    updatedRestored should haveExpectedBalance
  }

  it should "require payment for updates with non-zero balance" in (eachScenario where buyerDefined) { implicit scenario =>
    initialCheckout.transact(None) should be ('left)
  }

  it should "contain same line items as after most recent update" in (eachScenario where updateDefined) { implicit scenario =>
    val updatedSaved = transactedCheckout.updated.transacted
    val updatedRestored  = updatedSaved.restored

    updatedSaved.lineItems should haveLineItemEqualityTo (updatedRestored.lineItems)
  }

  it should "not do anything on transact without any changes being made" in (eachScenario where buyerAndPaymentDefined) {
    implicit scenario =>

    val transactedWithCash = restoredCheckout.transact(Some(randomCashTransactionType))
    val transactedWithoutCash = restoredCheckout.transact(None)

    transactedWithCash should be (Right(restoredCheckout))
    transactedWithoutCash should be (Right(restoredCheckout))

  }






  //
  // Helpers
  //
  def zeroDollars: Money = BigDecimal(0).toMoney()
  def haveExpectedBalance = Matcher { left: Checkout =>
    def expectedTotal = left.lineItems.notOfNatures(Summary, Payment).sumAmounts
    def totalPayments = left.payments.sumAmounts
    def expectedBalance = expectedTotal plus totalPayments

    haveAmount(expectedBalance).apply(left.balance)  // see LineItemMatchers
  }


  //
  // Scenario running helpers
  //
  val eachScenario = new ScenarioRunner(_ => true)

  class ScenarioRunner(predicate: Predicate){
    def apply(body: Body): Unit = for (scenario <- scenarios if predicate(scenario)) body(scenario)
    def and(nextPredicate: Predicate) = new ScenarioRunner( compose(predicate, nextPredicate) )
    def where(nextPredicate: Predicate): ScenarioRunner = and(nextPredicate)
  }


}
