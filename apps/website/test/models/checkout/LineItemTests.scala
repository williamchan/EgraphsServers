package models.checkout

import checkout.Conversions._
import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import LineItemMatchers._
import utils.TestData._
import utils.CanInsertAndUpdateAsThroughServicesWithLongKeyTests
import services.db.CanInsertAndUpdateAsThroughServices
import models.CashTransaction
import services.payment.StripeTestPayment
import services.AppConfig

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

  /** sets of types which should prevent resolution */
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

  def saveLineItem(item: ItemT): ItemT = item.transact(checkout).asInstanceOf[ItemT]

  lazy val checkout = newSavedCheckout()
}


/** tests simple LineItem persistence */
trait CanInsertAndUpdateAsThroughServicesWithLineItemEntityTests[
  ItemT <: LineItem[_] with CanInsertAndUpdateAsThroughServices[ItemT, LineItemEntity] with HasLineItemEntity
] extends CanInsertAndUpdateAsThroughServicesWithLongKeyTests[ItemT, LineItemEntity]
{ this: FlatSpec with ShouldMatchers with LineItemTests[_ <: LineItemType[_], ItemT] =>

  import LineItemTestData._

  // NOTE: casting is unfortunate but seems unavoidable
  override def newModel: ItemT = newLineItem
  override def transformModel(model: ItemT) = model.withAmount(model.amount.plus(1.0)).asInstanceOf[ItemT]
  override def restoreModel(id: Long): Option[ItemT] = restoreLineItem(id)
  override def saveModel(model: ItemT): ItemT = {
    if (model.id > 0) { model.update() } else {
      model.transact(newSavedCheckout()).asInstanceOf[ItemT]
    }
  }

}



















/**
 * Collection of helpers for generating line items and types of various sorts, primarily to use
 * for LineItemTests. Unless named as 'saved' whatever, these are not saved.
 */
object LineItemTestData {
  import services.Finance.TypeConversions._

  def seqOf[T](gen: => T)(n: Int): Seq[T] = (0 to n).toSeq.map(_ => gen)

  def randomGiftCertificateItem = randomGiftCertificateType.lineItems().get.head
  def randomGiftCertificateType = GiftCertificateLineItemType(generateFullname, randomMoney)

  def taxItemOn(subtotal: SubtotalLineItem): TaxLineItem = randomTaxType.lineItems(Seq(subtotal), Nil).get.head
  def randomTaxItem: TaxLineItem = taxItemOn(randomSubtotalItem)
  def randomTaxType: TaxLineItemType = TaxLineItemType("98888", randomTaxRate, Some("Test tax"))

  def randomSubtotalItem: SubtotalLineItem = SubtotalLineItem(randomMoney)
  def randomTotalItem = TotalLineItem(randomMoney)
  def randomBalanceItem = BalanceLineItem(randomMoney)

  def randomCashTransactionType = CashTransactionLineItemType(newSavedAccount().id, zipcode, Some(payment.testToken().id))
  def randomCashTransactionItem = randomCashTransactionType.lineItems(Seq(randomBalanceItem)).get.head

  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()
  def randomTaxRate = BigDecimal(random.nextInt(15).toDouble / 100)

  def newCheckout = Checkout(Seq(randomGiftCertificateType), zipcode, Some(newSavedCustomer()))
  def newSavedCheckout() = newCheckout.insert()
  def newTransactedCheckout = newCheckout.transact(Some(randomCashTransactionType))

  def payment: StripeTestPayment = {
    val pment = AppConfig.instance[StripeTestPayment]; pment.bootstrap(); pment
  }

  protected def zipcode = Some("98888")
}