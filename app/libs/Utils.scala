package libs

import java.sql.Timestamp
import java.util.Date

/**
 * Helpful utilities with no other place to call home
 */
object Utils {

  /**
   * Creates an Option out of any nullable type. Used most frequently in our
   * JPA models.
   *
   * @param toMakeOption the value to make an Option
   *
   * @return the Option of toMakeOption. This is None if toMakeOption was null,
   *   or if it was an empty String. Otherwise it's Some(toMakeOption)
   */
  def optional[T <: AnyRef](toMakeOption: T): Option[T] = {
    toMakeOption match {
      case null => None
      case aString: String if aString == "" => None
      case value => Some(value)
    }
  }
}