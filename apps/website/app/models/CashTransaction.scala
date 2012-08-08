package models

import enums._
import org.joda.money.{CurrencyUnit, Money}
import java.sql.Timestamp
import services.Time
import com.google.inject.Inject
import services.AppConfig
import services.db.{Schema, Saves, KeyedCaseClass}
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
case class CashTransaction(
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
    Money.of(CurrencyUnit.USD, amountInCurrency.bigDecimal)
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

class CashTransactionStore @Inject() (schema: Schema) extends Saves[CashTransaction] with SavesCreatedUpdated[CashTransaction] {

  def findByOrderId(orderId: Long): Query[CashTransaction] = {
    from(schema.cashTransactions)(txn =>
      where(txn.orderId === Some(orderId))
        select (txn)
    )
  }

  def findByPrintOrderId(printOrderId: Long): Query[CashTransaction] = {
    from(schema.cashTransactions)(txn =>
      where(txn.printOrderId === Some(printOrderId))
        select (txn)
    )
  }

  //
  // SavesCreatedUpdated[CashTransaction] members
  //
  override protected def withCreatedUpdated(toUpdate: CashTransaction, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

  //
  // Saves[CashTransaction] members
  //
  protected def table = schema.cashTransactions

  def defineUpdate(theOld: CashTransaction, theNew: CashTransaction) = {

    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.orderId := theNew.orderId,
      theOld.printOrderId := theNew.printOrderId,
      theOld.amountInCurrency := theNew.amountInCurrency,
      theOld.billingPostalCode := theNew.billingPostalCode,
      theOld.currencyCode := theNew.currencyCode,
      theOld._cashTransactionType := theNew._cashTransactionType,
      theOld.stripeCardTokenId := theNew.stripeCardTokenId,
      theOld.stripeChargeId := theNew.stripeChargeId,
      theOld.updated := theNew.updated
    )
  }
}
