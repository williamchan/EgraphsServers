package services.http


/**
 * Provides well-behaved access to Play request parameters as Option[] types.
 * Any null or empty-string option is None, any value is Some(theValue)
 */
class SafePlayParams(rawParams: SafePlayParams.OptionParamable) {

  // TODO: PLAY20 migration this method is no longer necessary.
  /** Deprecated because Play 2.0's `get` method already returns an option. */  
  @Deprecated
  def getOption(name: String): Option[String] = {
    rawParams.get(name)
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
  type OptionParamable = { def get(key: String): Option[String] }
  object Conversions {
    implicit def paramsToOptionalParams(rawParams: OptionParamable): SafePlayParams = {
      new SafePlayParams(rawParams)
    }
  }
}