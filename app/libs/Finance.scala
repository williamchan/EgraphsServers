package libs

import org.joda.money.{CurrencyUnit, Money}
import java.math.RoundingMode

/**
 * Utilities for working with [[org.joda.money.Money]].
 *
 * Most of the utilities are best used implicitly by importing Finance.TypeConversions._
 *
 * Example:
 * {{{
 *   import libs.Finance.TypeConversions._
 *
 *   // Make some Money objects from an amount
 *   val amount = BigDecimal(100.50)
 *
 *   val dollars = amount.toMoney()
 *   val euros = amount.toMoney(org.joda.money.CurrencyUnit.EUR)
 *
 *   // Format the money
 *   val forDisplay = dollars.formatSimply // -> $100.50
 *
 * }}}
 */
object Finance {

  /**
   * Implicit conversions to make formatting and translating money between types
   * more easy
   */
  object TypeConversions {

    /**
     * Pimped version of [[org.joda.money.Money]].
     */
    case class RichMoney(money: Money) {

      /**
       * Provides a sensible human formatting for money.
       *
       * Money(100) -> "$100"
       * Money(100.5) -> "$100.50"
       * Money(1) -> "$1".
       */
      def formatSimply = {
        val amount = money.getAmount
        
        val amountFormatted = if (money.getMinorPart == 0) {
          "%d" format amount.toBigInteger
        } else {
          "%.2f" format amount
        }

        money.getCurrencyUnit.getSymbol + amountFormatted
      }
    }

    /**
     * Provides explicit methods to translate `BigDecimals` into [[org.joda.money.Money]]
     */
    case class FinanceBigDecimal(bigDecimal: BigDecimal) {
      def toMoney(currency: CurrencyUnit = CurrencyUnit.USD): Money = {
        moneyFrom(bigDecimal, currency)
      }
    }

    implicit def bigDecimalToFinanceBigDecimal(bigDecimal: BigDecimal): FinanceBigDecimal = {
      FinanceBigDecimal(bigDecimal)
    }

    implicit def moneyToRichMoney(money: Money): RichMoney = {
      RichMoney(money)
    }

    def moneyFrom(amount: BigDecimal, currency: CurrencyUnit = CurrencyUnit.USD): Money = {
      Money.of(currency, amount.bigDecimal, RoundingMode.HALF_EVEN)
    }
  }
}