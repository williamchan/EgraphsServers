package models

import org.joda.money.CurrencyUnit
import services.AppConfig
import utils._

class CashTransactionTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[CashTransaction]
  with CreatedUpdatedEntityTests[Long, CashTransaction]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def store = AppConfig.instance[CashTransactionStore]

  //
  // SavingEntityTests[CashTransaction] methods
  //
  override def newEntity = {
    CashTransaction(accountId=TestData.newSavedAccount().id)
  }

  override def saveEntity(toSave: CashTransaction) = {
    store.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: CashTransaction) = {
    toTransform.copy(
      accountId = TestData.newSavedAccount().id,
      amountInCurrency = 1
    )
  }

  //
  // Test cases
  //

  "CashTransaction" should "require certain fields" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {CashTransaction().save()}
    exception.getLocalizedMessage should include("CashTransaction: type must be specified")
  }

  "A CashTransaction" should "have the correct cash value and currency type" in new EgraphsTestApplication {
    val amount: BigDecimal = 20.19
    val transaction = CashTransaction(amountInCurrency=amount)

    transaction.cash.getAmount should be (amount.bigDecimal)
    transaction.cash.getCurrencyUnit should be (CurrencyUnit.USD)
  }

}