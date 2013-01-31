package models.enums

import egraphs.playutils.Enum
import models.checkout._
import org.joda.money.{CurrencyUnit, Money}

sealed abstract class CheckoutCodeType(val name: String) { this: CodeTypeFactory[_, _] =>
  def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity): LineItem[_] =
    itemInstance(itemEntity, typeEntity)
}

/**
 * Allows CheckoutCodeType to be used to retrieve the "code type" of a LineItem (LineItemType could be added
 * as well if needed).
 */
trait CodeTypeFactory[TypeT <: LineItemType[_], ItemT <: LineItem[_]] {
  def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity): ItemT
}

object CheckoutCodeType extends Enum {
  protected type ItemEntity = LineItemEntity
  protected type TypeEntity = LineItemTypeEntity

  sealed abstract class EnumVal(name: String) extends CheckoutCodeType(name) with Value {
    this: CodeTypeFactory[_, _] =>
  }


  //
  // Products
  //
  val GiftCertificate =
    new EnumVal("GiftCertificateLineItemType") with CodeTypeFactory[GiftCertificateLineItemType, GiftCertificateLineItem] {
      override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = {
        GiftCertificateLineItem(itemEntity, typeEntity)
      }
    }

  //
  // Discounts
  //


  //
  // Charges
  //
  val CashTransaction =
    new EnumVal("CashTransactionLineItemType") with CodeTypeFactory[CashTransactionLineItemType, CashTransactionLineItem]{
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = {
      CashTransactionLineItem(itemEntity, typeEntity)
    }
  }

  //
  // Summaries
  //
  val Subtotal = new EnumVal("SubtotalLineItemType") with CodeTypeFactory[SubtotalLineItemType, SubtotalLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = {
      SubtotalLineItem( Money.of(
        CurrencyUnit.USD,
        itemEntity._amountInCurrency.bigDecimal
      ))
    }
  }

  val Total = new EnumVal("TotalLineItemType") with CodeTypeFactory[TotalLineItemType, TotalLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = {
      TotalLineItem( Money.of(
        CurrencyUnit.USD,
        itemEntity._amountInCurrency.bigDecimal
      ))
    }
  }

  val Balance = new EnumVal("BalanceLineItemType") with CodeTypeFactory[BalanceLineItemType, BalanceLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = {
      BalanceLineItem(itemEntity.amount)
    }
  }

  //
  // Taxes
  //
  val Tax = new EnumVal("TaxLineItemType") with CodeTypeFactory[TaxLineItemType, TaxLineItem] {
    override def itemInstance(itemEntity: ItemEntity, typeEntity: TypeEntity) = {
      TaxLineItem(itemEntity, typeEntity)
    }
  }
}



trait HasCodeType { def codeType: CheckoutCodeType }
