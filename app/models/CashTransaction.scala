package models

import org.joda.money.{CurrencyUnit, Money}
import java.sql.Timestamp
import services.{Utils, Time}
import com.google.inject.Inject
import services.AppConfig
import services.db.{Schema, Saves, KeyedCaseClass}

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
  orderId: Option[Long] = None,
  amountInCurrency: BigDecimal = 0,
  currencyCode: String = CurrencyUnit.USD.getCode,
  typeString: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CashTransactionServices = AppConfig.instance[CashTransactionServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import CashTransaction._

  //
  // Public members
  //
  /** Persist the transaction */
  def save(): CashTransaction = {
    require(!typeString.isEmpty, "CashTransaction: type must be specified")
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

  def transactionType: TransactionType = {
    CashTransaction.types(typeString)
  }

  def withType(newType: TransactionType): CashTransaction = {
    copy(typeString=newType.value)
  }

  //
  // KeyedCaseClass[Long] members
  //
  override def unapplied = {
    CashTransaction.unapply(this)
  }
}

object CashTransaction {
  //
  // Public members
  //
  sealed abstract class TransactionType(val value: String)
  case object EgraphPurchase extends TransactionType("EgraphPurchase")
  case object PurchaseRefund extends TransactionType("PurchaseRefund")
  case object CelebrityDisbursement extends TransactionType("CelebrityDisbursement")

  val types = Utils.toMap[String, TransactionType](Seq(
    EgraphPurchase,
    PurchaseRefund,
    CelebrityDisbursement
  ), key=(theType) => theType.value)
}

class CashTransactionStore @Inject() (schema: Schema) extends Saves[CashTransaction] with SavesCreatedUpdated[CashTransaction] {
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
    import org.squeryl.PrimitiveTypeMode._

    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.orderId := theNew.orderId,
      theOld.currencyCode := theNew.currencyCode,
      theOld.amountInCurrency := theNew.amountInCurrency,
      theOld.typeString := theNew.typeString,
      theOld.updated := theNew.updated
    )
  }
}

