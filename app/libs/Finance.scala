package libs

import org.joda.money.{CurrencyUnit, Money}
import java.math.RoundingMode

object Finance {

  object TypeConversions {
    case class FinanceBigDecimal(bigDecimal: BigDecimal) {
      def toMoney(currency: CurrencyUnit = CurrencyUnit.USD): Money = {
        moneyFrom(bigDecimal, currency)
      }
    }

    implicit def bigDecimalToFinanceBigDecimal(bigDecimal: BigDecimal): FinanceBigDecimal = {
      FinanceBigDecimal(bigDecimal)
    }

    def moneyFrom(amount: BigDecimal, currency: CurrencyUnit = CurrencyUnit.USD): Money = {
      Money.of(currency, amount.bigDecimal, RoundingMode.HALF_EVEN)
    }
  }
}