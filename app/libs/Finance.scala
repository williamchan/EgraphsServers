package libs

import org.joda.money.{CurrencyUnit, Money}
import java.math.RoundingMode

object Finance {
  def dollarsFrom(amount: BigDecimal): Money = {
    Money.of(CurrencyUnit.USD, amount.bigDecimal, RoundingMode.HALF_EVEN)
  }

  object TypeConversions {
    case class FinanceBigDecimal(bigDecimal: BigDecimal) {
      def toDollars: Money = {
        Finance.dollarsFrom(bigDecimal)
      }
    }

    implicit def bigDecimalToFinanceBigDecimal(bigDecimal: BigDecimal): FinanceBigDecimal = {
      FinanceBigDecimal(bigDecimal)
    }
  }
}