package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import org.joda.money.CurrencyUnit
import services.AppConfig

class CashTransactionTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[CashTransaction]
  with CreatedUpdatedEntityTests[CashTransaction]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val store = AppConfig.instance[CashTransactionStore]

  //
  // SavingEntityTests[CashTransaction] methods
  //
  override def newEntity = {
    CashTransaction(accountId=Account().save().id)
  }

  override def saveEntity(toSave: CashTransaction) = {
    store.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: CashTransaction) = {
    toTransform.copy(
      accountId = Account(email="derp").save().id,
      amountInCurrency = 1
    ).withType(CashTransaction.EgraphPurchase)
  }

  //
  // Test cases
  //
  "A CashTransaction" should "have the correct cash value and currency type" in {
    val amount: BigDecimal = 20.19
    val transaction = CashTransaction(amountInCurrency=amount)

    transaction.cash.getAmount should be (amount.bigDecimal)
    transaction.cash.getCurrencyUnit should be (CurrencyUnit.USD)
  }

}