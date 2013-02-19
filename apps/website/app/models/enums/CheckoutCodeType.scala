package models.enums

import egraphs.playutils.Enum
import models.checkout._
import org.joda.money.{CurrencyUnit, Money}

/**
 * Enum that requires the enumerated values to implement a method for creating a concrete LineItem out of a LineItem and
 * LineItemType entity.
 *
 * TODO: could remove LineItemType parameter from CodeTypeFactory unless it gets used for a "typeInstance" method
 */
sealed abstract class CheckoutCodeType(val name: String) { this: OfCheckoutClass[_, _] =>
  def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity): LineItem[_]
}


/** trait for tagging type parameters onto the enum, which didn't seem to want to work directly. */
trait OfCheckoutClass[TypeT <: LineItemType[_], ItemT <: LineItem[_]] { this: CheckoutCodeType => }


/** Helper for CodeTypes whose companions have an apply method that mirrors the use of itemInstance */
trait CodeTypeFactory[TypeT <: LineItemType[_], ItemT <: LineItem[_]] extends OfCheckoutClass[TypeT, ItemT] {
  this: CheckoutCodeType =>

  def itemCompanion: { def apply(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity): ItemT }

  override def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity): ItemT = {
    itemCompanion(itemEntity, typeEntity)
  }
}


object CheckoutCodeType extends Enum {
  protected type ItemEntity = LineItemEntity
  protected type TypeEntity = LineItemTypeEntity

  sealed abstract class EnumVal(name: String) extends CheckoutCodeType(name) with Value {
    this: OfCheckoutClass[_, _] =>
  }

  //
  // Products
  //
  val GiftCertificate = new EnumVal("GiftCertificateLineItem") with CodeTypeFactory[GiftCertificateLineItemType, GiftCertificateLineItem] {
    override def itemCompanion = GiftCertificateLineItem
  }

  val EgraphOrder = new EnumVal("EgraphOrderLineItem") with CodeTypeFactory[EgraphOrderLineItemType, EgraphOrderLineItem] {
    override def itemCompanion = EgraphOrderLineItem
  }
  
  val PrintOrder = new EnumVal("PrintOrderLineItem") with CodeTypeFactory[PrintOrderLineItemType, PrintOrderLineItem] {
    override def itemCompanion = PrintOrderLineItem
  }

  //
  // Discounts
  //
  val Coupon = new EnumVal("CouponLineItem") with CodeTypeFactory[CouponLineItemType, CouponLineItem] {
    override def itemCompanion = CouponLineItem
  }

  //
  // Charges
  //
  val CashTransaction =
    new EnumVal("CashTransactionLineItem") with CodeTypeFactory[CashTransactionLineItemType, CashTransactionLineItem]{
      override def itemCompanion = CashTransactionLineItem
  }

  //
  // Summaries
  //
  val Subtotal = new EnumVal("SubtotalLineItem") with OfCheckoutClass[SubtotalLineItemType, SubtotalLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = SubtotalLineItem(itemEntity.amount)
  }

  val Total = new EnumVal("TotalLineItem") with OfCheckoutClass[TotalLineItemType, TotalLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = TotalLineItem(itemEntity.amount)
  }

  val Balance = new EnumVal("BalanceLineItem") with OfCheckoutClass[BalanceLineItemType, BalanceLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) =  BalanceLineItem(itemEntity.amount)
  }

  //
  // Taxes
  //
  val Tax = new EnumVal("TaxLineItem") with CodeTypeFactory[TaxLineItemType, TaxLineItem] {
    override def itemCompanion = TaxLineItem
  }
}


trait HasCodeType { def codeType: CheckoutCodeType }