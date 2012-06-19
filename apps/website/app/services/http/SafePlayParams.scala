package services.http


/**
 * Provides well-behaved access to Play request parameters as Option[] types.
 * Any null or empty-string option is None, any value is Some(theValue)
 */
class SafePlayParams(rawParams: SafePlayParams.OptionParamable) {

  /** See class definition */
  def getOption(name: String): Option[String] = {
    Option(rawParams.get(name)) match {
      case None | Some("") =>
        None

      case someValue =>
        someValue
    }
  }

  def getLongOption(name: String): Option[Long] = {
    getOption(name) match {
      case None => None
      case Some(value) => try {
        Some(value.toLong)
      } catch {
        case e: NumberFormatException => None
      }
    }
  }
}

object SafePlayParams {
  type OptionParamable = { def get(key: String): String }
  object Conversions {
    implicit def paramsToOptionalParams(rawParams: OptionParamable): SafePlayParams = {
      new SafePlayParams(rawParams)
    }
  }
}