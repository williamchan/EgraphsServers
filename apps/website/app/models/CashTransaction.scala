package models

import enums._
import org.joda.money.{CurrencyUnit, Money}
import java.sql.Timestamp
import services.Time
import com.google.inject.Inject
import services.AppConfig
import services.db.{Schema, SavesWithLongKey, KeyedCaseClass}
import services.Finance.TypeConversions._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query

/**
 * Services used by every instance of CashTransaction
 */
case class CashTransactionServices @Inject() (cashTransactionStore: CashTransactionStore)

/**
 * Represents a single payment event relative to Egraphs. There should be a row in
 * the corresponding table for every in-domain purchase and payment. Positive amounts
 * add to our account (e.g. Egraph payments), negative amounts decrease it (e.g. Celebrity
 * Disbursements).
 */
case class  CashTransaction(
  id: Long = 0,
  accountId: Long = 0,
  orderId: Option[Long] = None,       // This should probably be a oneToMany
  printOrderId: Option[Long] = None,  // This should probably be a oneToMany
  amountInCurrency: BigDecimal = 0,
  billingPostalCode: Option[String] = None,
  currencyCode: String = CurrencyUnit.USD.getCode,
  _cashTransactionType: String = "",
  stripeCardTokenId: Option[String] = None,
  stripeChargeId: Option[String] = None,
  lineItemId: Option[Long] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CashTransactionServices = AppConfig.instance[CashTransactionServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasCashTransactionType[CashTransaction]
{

  //
  // Public members
  //
  /** Persist the transaction */
  def save(): CashTransaction = {
    require(!_cashTransactionType.isEmpty, "CashTransaction: type must be specified")
    services.cashTransactionStore.save(this)
  }

  /** Return the [[org.joda.money.Money]] representation of the object. */
  def cash: Money = {
    amountInCurrency.toMoney()
  }

  /** Returns the transaction with a new cash amount */
  def withCash(newCash: Money): CashTransaction = {
    copy(
      amountInCurrency=BigDecimal(newCash.getAmount),
      currencyCode=newCash.getCurrencyUnit.getCode
    )
  }

  //
  // KeyedCaseClass[Long] members
  //
  override def unapplied = CashTransaction.unapply(this)

  override def withCashTransactionType(enum: CashTransactionType.EnumVal) = {
    this.copy(_cashTransactionType = enum.name)
  }
}

class CashTransactionStore @Inject() (schema: Schema)
  extends SavesWithLongKey[CashTransaction] with SavesCreatedUpdated[CashTransaction]
{

  def findByOrderId(orderId: Long): List[CashTransaction] = {
    from(schema.cashTransactions)(txn =>
      where(txn.orderId === Some(orderId))
        select (txn)
    ).toList
  }

  def findByPrintOrderId(printOrderId: Long): Query[CashTransaction] = {
    from(schema.cashTransactions)(txn =>
      where(txn.printOrderId === Some(printOrderId))
        select (txn)
    )
  }

  def findByLineItemId(lineItemId: Long): Option[CashTransaction] = {
    from(schema.cashTransactions)(txn =>
      where(txn.lineItemId === Some(lineItemId))
        select (txn)
    ).headOption
  }

  //
  // SavesCreatedUpdated[CashTransaction] members
  //
  override protected def withCreatedUpdated(toUpdate: CashTransaction, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

  //
  // SavesWithLongKey[CashTransaction] members
  //
  protected def table = schema.cashTransactions

}
