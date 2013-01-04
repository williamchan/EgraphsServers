package models.checkout

import models.{CashTransactionStore, CashTransaction}
import models.enums.{CashTransactionType, CodeType, LineItemNature}
import org.joda.money.Money
import scalaz.Lens
import services.db.{QueriesAsEntity, CanInsertAndUpdateAsThroughServices, Schema}
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import com.google.inject.Inject
import play.api.libs.json.Json



trait CashTransactionLineItemType extends LineItemType[CashTransaction] with HasLineItemTypeEntity {
  override def nature = LineItemNature.Charge

  // expected constructor arguments
  def accountId: Long
  def billingPostalCode: Option[String]
  def stripeCardTokenId: Option[String]

  override def codeType = _entity.codeType
}

case class StripeCashTransactionLineItemType (
  accountId: Long,
  billingPostalCode: Option[String],
  stripeCardTokenId: Option[String],
  services: CashTransactionLineItemTypeServices = AppConfig.instance[CashTransactionLineItemTypeServices]
) extends CashTransactionLineItemType {

  override lazy val _entity = StripeCashTransactionLineItemType.entity

  override def toJson = ""

  override def description = "Line item type for cash transaction made through Stripe"



  // lineItems makes line item for total from resolvedItems
  override def lineItems(resolved: Seq[LineItem[_]], unresolved: Seq[LineItemType[_]])
  : Seq[StripeCashTransactionLineItem] = {
    for(total <- resolved.filter(item => item.codeType == CodeType.Total)) yield {
      require(total.amount.isZero || stripeCardTokenId.isDefined, "Stripe card token required for non-zero totals.")

      val txn = CashTransaction(
        accountId = accountId,
        billingPostalCode = billingPostalCode,
        stripeCardTokenId = stripeCardTokenId
      ) .withCashTransactionType(CashTransactionType.Checkout)
        .withCash(total.amount)

      StripeCashTransactionLineItem(this, txn)
    }
  }


}

object StripeCashTransactionLineItemType {
  type CashTransLITServices = CashTransactionLineItemTypeServices

  def apply(cashTransaction: CashTransaction) = {
    new StripeCashTransactionLineItemType(
      cashTransaction.accountId,
      cashTransaction.billingPostalCode,
      cashTransaction.stripeCardTokenId
    )
  }


  // TODO(SER-499): would like this to be a lazy val... maybe move into store or services
  protected def entity(
    implicit services: CashTransLITServices = AppConfig.instance[CashTransLITServices]
  ): LineItemTypeEntity = {
    LineItemTypeEntity(
      desc = CodeType.StripeCashTransaction.name + " entity",
      nature = LineItemNature.Charge,
      codeType = CodeType.StripeCashTransaction
    )
  }

}


















case class CashTransactionLineItemTypeServices @Inject() (
  schema: Schema
) {

}


class CashTransactionLineItemTypeStore @Inject() (
  schema: Schema
) {

}