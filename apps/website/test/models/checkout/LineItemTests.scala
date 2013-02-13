package models.checkout

import checkout.Conversions._
import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import LineItemMatchers._
import utils.TestData
import utils.CanInsertAndUpdateEntityWithLongKeyTests
import services.db.{CanInsertAndUpdateEntityThroughTransientServices, CanInsertAndUpdateEntityThroughServices}
import models.{Coupon, CashTransaction}
import services.payment.StripeTestPayment
import services.AppConfig

/** mixin for testing a LineItemType's lineItems method */
trait LineItemTests[TypeT <: LineItemType[_], ItemT <: LineItem[_]] {
  this: FlatSpec with ShouldMatchers =>
  import LineItemTestData._
  import TestData._

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

    hasExpectedRestoredDomainObject(restored) should be (true)
  }


  //
  // Helpers
  //
  def newLineItem: ItemT = newItemType.lineItems(resolvableItemSets.head, Nil)
    .get.head.asInstanceOf[ItemT]

  def saveLineItem(item: ItemT): ItemT = item match {
    case subItem: SubLineItem[_] => subItem.transactAsSubItem(checkout).asInstanceOf[ItemT]
    case lineItem: LineItem[_] => lineItem.transact(checkout).asInstanceOf[ItemT]
  }

  lazy val checkout = newSavedCheckout()

  lazy val lineItemStore = AppConfig.instance[LineItemStore]
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





















/**
 * Collection of helpers for generating line items and types of various sorts, primarily to use
 * for LineItemTests. Unless named as 'saved' whatever, these are not saved.
 */
object LineItemTestData {
  import services.Finance.TypeConversions._
  import TestData._

  def seqOf[T](gen: => T)(n: Int): Seq[T] = (0 to n).toSeq.map(_ => gen)


  def randomEgraphOrderType = EgraphOrderLineItemType(
    productId = newSavedProduct().id,
    recipientName = generateFullname()
  )
  def randomEgraphOrderItem = randomEgraphOrderType.lineItems(Nil, Nil).get.head

  def randomPrintOrderType = PrintOrderLineItemType(newSavedOrder())
  def randomPrintOrderItem = randomPrintOrderType.lineItems(Nil, Nil).get.head

  def randomGiftCertificateItem = randomGiftCertificateType.lineItems().get.head
  def randomGiftCertificateType = GiftCertificateLineItemType(generateFullname, randomMoney)

  def randomCouponType(coupon: Coupon = newSavedCoupon()) = {
    val couponTypeServices = AppConfig.instance[CouponLineItemTypeServices]
    couponTypeServices.findByCouponCode(coupon.code).get
  }

  def taxItemOn(subtotal: SubtotalLineItem): TaxLineItem = randomTaxType.lineItems(Seq(subtotal), Nil).get.head
  def randomTaxItem: TaxLineItem = taxItemOn(randomSubtotalItem)
  def randomTaxType: TaxLineItemType = TaxLineItemType("98888", randomTaxRate, Some("Test tax"))

  def randomSubtotalItem: SubtotalLineItem = SubtotalLineItem(randomMoney)
  def randomTotalItem = TotalLineItem(randomMoney)
  def randomBalanceItem = BalanceLineItem(randomMoney)

  def randomCashTransactionType = CashTransactionLineItemType(Some(payment.testToken().id), zipcode)
  def randomCashTransactionItem = randomCashTransactionType.lineItems(Seq(randomBalanceItem)).get.head

  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()
  def randomTaxRate = BigDecimal(random.nextInt(15).toDouble / 100)


  def newCheckout: FreshCheckout = {
    val account = Some(newSavedAccount())
    val customer = newSavedCustomer(account)
    val address = newSavedAddress(account)
    Checkout.create(Seq(randomGiftCertificateType), Some(customer), address)
  }
  def newSavedCheckout() = newCheckout.insert()
  def newTransactedCheckout() = newCheckout.transact(Some(randomCashTransactionType))

  def payment: StripeTestPayment = {
    val pment = AppConfig.instance[StripeTestPayment]; pment.bootstrap(); pment
  }

  protected def zipcode = Some("98888")
}