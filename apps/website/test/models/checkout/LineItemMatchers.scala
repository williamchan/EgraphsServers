package models.checkout

import org.joda.money.Money
import org.scalatest.matchers.{MatchResult, Matcher}
import models.checkout.checkout.Conversions._

object LineItemMatchers {
  /**
   * Checks that the left LineItems are a subset of the otherItems; semi-hacky way to check for
   * equivalence between two sets of line items (should test in both direction to establish equality).
   *
   * @param otherItems line items which is a super set of left if a positive match is made
   * @param checkoutState description of the left line items for messages
   * @return MatchResult; positive if left is subset of otherItems
   */
  def beContainedIn(otherItems: LineItems, checkoutState: String = "other") = Matcher { left: LineItems =>
    val notInOtherItems = left.filterNot { item => otherItems.exists(item equalsLineItem _) }
    val successMessage = "All items were contained in %s checkout".format(checkoutState)
    val failMessage = "Line items with id's (%s) were not contained in %s checkout".format(
      notInOtherItems.map(item => item.id).mkString(", "),
      checkoutState
    )

    MatchResult(notInOtherItems.isEmpty, failMessage, successMessage)
  }

  /** Convenience matcher for checking amount of line item */
  def haveAmount(desiredAmount: Money) = Matcher { left: LineItem[_] =>
    MatchResult(left.amount == desiredAmount,
      (left.amount + " did not equal " + desiredAmount),
      "LineItem has desired amount"
    )
  }

  /** Convenience matchers for comparing amounts of line items */
  def haveAmountOf(right: LineItem[_]) = haveAmount(right.amount)
  def haveNegatedAmountOf(right: LineItem[_]) = haveAmount(right.amount.negated)




  /** Checks is a given LineItemType resolves any LineItems from the given items and types */
  def resolveFrom(resolved: LineItems, unresolved: LineItemTypes) = Matcher { left: LineItemType[_] =>
    val result = left.lineItems(resolved, unresolved)
    val resultStr =     "**result:     " + result
    val resolvedStr =   "**resolved:   " + resolved
    val unresolvedStr = "**unresolved: " + unresolved
    val failMessage = "Did not resolve from:\n" + resolvedStr + "\n" + unresolvedStr
    val successMessage = "Resolved:\n" + resultStr + "\nfrom:\n" + resolvedStr + "\n" + unresolvedStr
    MatchResult (result.isDefined, failMessage, successMessage)
  }

}
