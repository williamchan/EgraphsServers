package services.http

import play.mvc.Scope.Params

/**
 * Provides well-behaved access to Play request parameters as Option[] types.
 * Any null or empty-string option is None, any value is Some(theValue)
 */
class OptionParams(rawParams: Params) {

  /** See class definition */
  def getOption(name: String): Option[String] = {
    Option(rawParams.get(name)) match {
      case None | Some("") =>
        None

      case someValue =>
        someValue
    }
  }
}

object OptionParams {
  object Conversions {
    implicit def paramsToOptionalParams(rawParams: Params): OptionParams = {
      new OptionParams(rawParams)
    }
  }
}