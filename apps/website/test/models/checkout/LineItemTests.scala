package models.checkout

import Conversions._
import org.scalatest.matchers.{Matcher, MatchResult, ShouldMatchers}
import org.scalatest.FlatSpec
import LineItemMatchers._
import utils.TestData
import utils.CanInsertAndUpdateEntityWithLongKeyTests
import services.payment.{Payment, YesMaamPayment, StripeTestPayment}
import services.AppConfig
import services.db.{CanInsertAndUpdateEntityThroughTransientServices, CanInsertAndUpdateEntityThroughServices}
import models.{Coupon, CashTransaction}
import models.enums.OfCheckoutClass
import models.enums.CheckoutCodeType

/** mixin for testing a LineItemType's lineItems method */
trait LineItemTests[TypeT <: LineItemType[_], ItemT <: LineItem[_]] {
  this: FlatSpec with ShouldMatchers =>
  import LineItemTestData._

  //
  // Abstract LineItemType-related members
  //
  /** create a new instance of LineItemType to test against */
  def newItemType: TypeT

  /** sets of items for which lineItems should resolve */
  def resolvableItemSets: Seq[LineItems]
  /** sets of types which should prevent resolution, can be Nil */
  def resolutionBlockingTypes: Seq[LineItemTypes]
  /** sets of types that should not interfere with resolution */
  def nonResolutionBlockingTypes: Seq[LineItemTypes]

  //
  // Abstract LineItem-related members
  //
  /** restores a transacted line item */
  def restoreLineItem(id: Long): Option[ItemT]
  /** checks that line item has roughly the expected domain object once restored */
  def hasExpectedRestoredDomainObject(lineItem: ItemT): Boolean

  //
  // Test cases
  //
  "A LineItemType" should "not resolve when needed types are also unresolved" in {
    // Rather exhaustively check that the LineItemType doesn't resolve in scenarios when it shouldn't.
    for (blockers <- resolutionBlockingTypes) {
      newItemType should not (resolveFrom(Nil, blockers))

      for (resolvables <- resolvableItemSets) {
        newItemType should not (resolveFrom(resolvables, blockers))

        for (nonblockers <- nonResolutionBlockingTypes) {
          val allUnresolved = blockers ++ nonblockers

          newItemType should not (resolveFrom(Nil, allUnresolved))
          newItemType should not (resolveFrom(resolvables, allUnresolved))
        }
      }
    }
  }

  it should "resolve when required items are resolved and no blocking types are unresolved" in {
    // Check that LineItemType resolves in several scenarios that it should
    for (resolvables <- resolvableItemSets) {
      newItemType should resolveFrom(resolvables, Nil)

      for (nonblockers <- nonResolutionBlockingTypes) {
        newItemType should resolveFrom(resolvables, nonblockers)
      }
    }
  }

  "A LineItem" should "have the expected domain object when restored" in {
    val restored = restoreLineItem(saveLineItem(newLineItem).id).get

    restored should haveExpectedRestoredDomainObject
  }

  //
  // Helpers
  //
  def resolve(itemType: TypeT) = itemType match {
    case subType: SubLineItemType[_] => subType.lineItemsAsSubType
    case _ => itemType.lineItems(resolvableItemSets.head, Nil).get
  }

  def newLineItem: ItemT = resolve(newItemType).ofCodeType(
    newItemType.codeType.asInstanceOf[CheckoutCodeType with OfCheckoutClass[TypeT, ItemT]]
  ).head

  def saveLineItem(item: ItemT): ItemT = item match {
    case subItem: SubLineItem[_] => subItem.transactAsSubItem(checkout).asInstanceOf[ItemT]
    case lineItem: LineItem[_] => lineItem.transact(checkout).asInstanceOf[ItemT]
  }

  lazy val checkout = newSavedCheckout()

  lazy val lineItemStore = AppConfig.instance[LineItemStore]


  def haveExpectedRestoredDomainObject = Matcher { left: ItemT =>
    MatchResult(
      hasExpectedRestoredDomainObject(left),
      "Bad bad, wtf is this: " + left.domainObject,
      "Good good."
    )
  }
}

/** tests simple LineItem persistence */
trait SavesAsLineItemEntityThroughServicesTests[
  ItemT <: LineItem[_] with SavesAsLineItemEntityThroughServices[ItemT, ServicesT] with HasLineItemEntity[ItemT],
  ServicesT <: SavesAsLineItemEntity[ItemT]
] extends CanInsertAndUpdateEntityWithLongKeyTests[ItemT, LineItemEntity]
{ this: FlatSpec with ShouldMatchers with LineItemTests[_ <: LineItemType[_], ItemT] =>

  import LineItemTestData._

  // NOTE: casting is unfortunate but seems unavoidable
  override def newModel: ItemT = newLineItem
  override def transformModel(model: ItemT) = model.withAmount(model.amount.plus(1.0)).asInstanceOf[ItemT]
  override def restoreModel(id: Long): Option[ItemT] = restoreLineItem(id)
  override def saveModel(model: ItemT): ItemT = saveLineItem(model)
}
