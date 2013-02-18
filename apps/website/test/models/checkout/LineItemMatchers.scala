package models.checkout

import org.joda.money.Money
import org.scalatest.matchers.{MatchResult, Matcher}
import models.checkout.checkout.Conversions._
import models.enums.{LineItemNature, CheckoutCodeType}
import services.Finance.TypeConversions._

object LineItemMatchers {
  /**
   * Checks that the left LineItems are a subset of the otherItems; hacky way to check for
   * equivalence between two sets of line items (should test in both direction to establish equality).
   *
   * @param rightItems line items which is a super set of left if a positive match is made
   * @return positive MatchResult if left is subset of otherItems
   */
  def beContainedIn(rightItems: LineItems) = Matcher { leftItems: LineItems =>
    val (leftUnmatched, rightUnmatched) = unmatchedItems(leftItems, rightItems)
    val successMessage = "Items where \"equal\""
    val failMessage = "Not found in right: %s\n\nNot found in left: %s\n\n".format(leftUnmatched, rightUnmatched)
    MatchResult(leftUnmatched.isEmpty && rightUnmatched.isEmpty, failMessage, successMessage)
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



  /** for comparing and finding disparities between sets of items that should be equal */
  private def unmatchedItems(leftItems: LineItems, rightItems: LineItems): (Seq[Any], Seq[Any]) = {
    type Item = LineItem[_]
    def unpack(items: LineItems) = for (item <- items) yield item.unpacked
    def in(items: LineItems) = (item: Item) => items exists { item.equalsLineItem(_) }

    val leftNotInRight = leftItems filterNot in(rightItems)
    val rightNotInLeft = rightItems filterNot in(leftItems)

    (unpack(leftNotInRight), unpack(rightNotInLeft))
  }
}
