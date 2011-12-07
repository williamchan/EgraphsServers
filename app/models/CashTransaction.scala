package models

import org.joda.money.{CurrencyUnit, Money}
import java.sql.Timestamp
import db.{Saves, KeyedCaseClass}
import libs.{Utils, Time}

/**
 * Represents a single payment event relative to Egraphs. There should be a line here
 * for every in-domain purchase and payment. Positive amounts add to our account,
 * negative amounts decrease it.
 */
case class CashTransaction(
  id: Long = 0,
  accountId: Long = 0,
  amountInCurrency: BigDecimal = 0,
  currencyCode: String = CurrencyUnit.USD.getCode,
  typeString: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import CashTransaction._

  //
  // Public members
  //
  /** Persist the transaction */
  def save(): CashTransaction = {
    CashTransaction.save(this)
  }

  /** Return the [[org.joda.money.Money]] representation of the object. */
  def money: Money = {
    Money.of(CurrencyUnit.USD, amountInCurrency.bigDecimal)
  }

  /** Returns the transaction with a new cash amount */
  def withMoney(newMoney: Money): CashTransaction = {
    copy(
      amountInCurrency=BigDecimal(newMoney.getAmount),
      currencyCode=newMoney.getCurrencyUnit.getCode
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

object CashTransaction extends Saves[CashTransaction] with SavesCreatedUpdated[CashTransaction] {
  //
  // Public members
  //
  sealed abstract class TransactionType(val value: String)
  case object EgraphPurchase extends TransactionType("EgraphPurchase")
  case object CelebrityDisbursement extends TransactionType("CelebrityDisbursement")
  
  val types = Utils.toMap[String, TransactionType](Seq(
    EgraphPurchase,
    CelebrityDisbursement
  ), key=(theType) => theType.value)
  
  //
  // SavesCreatedUpdated[CashTransaction] members
  //
  override protected def withCreatedUpdated(toUpdate: CashTransaction, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

  //
  // Saves[CashTransaction] members
  //
  protected def table = db.Schema.cashTransactions

  def defineUpdate(theOld: CashTransaction, theNew: CashTransaction) = {
    import org.squeryl.PrimitiveTypeMode._

    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.currencyCode := theNew.currencyCode,
      theOld.amountInCurrency := theNew.amountInCurrency,
      theOld.typeString := theNew.typeString,
      theOld.updated := theNew.updated
    )
  }

}