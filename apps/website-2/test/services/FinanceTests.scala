package services

import org.joda.money.CurrencyUnit
import utils.EgraphsUnitTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FinanceTests extends EgraphsUnitTest {
  import Finance.TypeConversions._

  val amount: BigDecimal = 1000.51

  "toMoney" should "turn BigDecimals into dollars by default" in {
    amount.toMoney().getCurrencyUnit should be (CurrencyUnit.USD)
  }

  it should "support other currency types" in {
    amount.toMoney(CurrencyUnit.EUR).getCurrencyUnit should be (CurrencyUnit.EUR)
  }

  it should "have the correct major and minor amounts" in {
    amount.toMoney().getAmount should be (amount.bigDecimal)
    amount.toMoney().getAmountMajor should be (BigDecimal(1000).bigDecimal)
    amount.toMoney().getAmountMinor should be (BigDecimal(100051).bigDecimal)
  }

  "formatSimply" should "omit cents if the amount has no decimals" in {
    BigDecimal(1000.00).toMoney().formatSimply should be ("$1000")
  }

  it should "include cents if the amount has decimals" in {
    BigDecimal(1000.50).toMoney().formatSimply should be ("$1000.50")
    BigDecimal(1000.51).toMoney().formatSimply should be ("$1000.51")
  }
}