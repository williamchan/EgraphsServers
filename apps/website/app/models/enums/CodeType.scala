package models.enums

import egraphs.playutils.Enum
import models.checkout._
import org.joda.money.{CurrencyUnit, Money}

sealed abstract class CodeType(val name: String)

// TODO(SER-499): remove LIT if no use for it comes up, test the rest of this (type erasure might fuck it up)
trait CodeTypeFactory[TypeT <: LineItemType[_], ItemT <: LineItem[_]] {
  this: CodeType =>
  // take both entities because they're both easily available when restoring
  // and both have important data
  def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity): ItemT
}

object CodeType extends Enum {

  sealed abstract class EnumVal(name: String) extends CodeType(name) with Value {
    this: CodeTypeFactory[_, _] =>
  }

  //
  // Products
  //
  /** NOTE: GiftCertificateLineItem and Type are products when purchased, coupons when used */
  val GiftCertificate = new EnumVal("GiftCertificateLineItemType")
    with CodeTypeFactory[GiftCertificateLineItemType, GiftCertificateLineItem]
  {
    override def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
      GiftCertificateLineItem(itemEntity, typeEntity)
    }
  }

  //
  // Discounts
  //


  //
  // Charges
  //
  val CashTransaction = new EnumVal("CashTransactionLineItemType")
    with CodeTypeFactory[CashTransactionLineItemType, CashTransactionLineItem]
  {
    override def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
      CashTransactionLineItem(itemEntity, typeEntity)
    }
  }

  //
  // Summaries
  //
  val Subtotal = new EnumVal("SubtotalLineItemType") with CodeTypeFactory[SubtotalLineItemType, SubtotalLineItem] {
    override def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
      SubtotalLineItem( Money.of(
        CurrencyUnit.USD,
        itemEntity._amountInCurrency.bigDecimal
      ))
    }
  }

  val Total = new EnumVal("TotalLineItemType") with CodeTypeFactory[TotalLineItemType, TotalLineItem] {
    override def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
      TotalLineItem( Money.of(
        CurrencyUnit.USD,
        itemEntity._amountInCurrency.bigDecimal
      ))
    }
  }

  //
  // Taxes
  //
  val Tax = new EnumVal("TaxLineItemType") with CodeTypeFactory[TaxLineItemType, TaxLineItem] {
    override def itemInstance(itemEntity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
      TaxLineItem(itemEntity, typeEntity)
    }
  }
}
