package services.http.forms

import play.api.data._
import play.api.data.format.Formatter
import play.api.data.format.Formats
import play.api.data.{FormError => PlayFormError}

/** These formatters were copied wholesale from Play 2.1. Deprecate them once we switch. */
object Play2FormFormatters {
  
  /**
   * Default formatter for the `Double` type.
   * 
   * This was copy/pasted from Play 2.1 code. Once we move to Play 2.1 it
   * will no longer be necessary and should be deleted.
   *
   */
  implicit def doubleFormat: Formatter[Double] = new Formatter[Double] {

    override val format = Some("format.real", Nil)

    def bind(key: String, data: Map[String, String]) =
      parsing(_.toDouble, "error.real", Nil)(key, data)

    def unbind(key: String, value: Double) = Map(key -> value.toString)
  }
  
  /**
   * Helper for formatters binders
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param error Error to set in case of parsing failure
   * @param key Key name of the field to parse
   * @param data Field data
   */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[PlayFormError], T] = {
    Formats.stringFormat.bind(key, data).right.flatMap { s =>
      util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(PlayFormError(key, errMsg, errArgs)))
    }
  }  

}