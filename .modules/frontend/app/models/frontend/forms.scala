package models.frontend.forms

import play.templates.Html


case class FormError(description: String) {
  override def toString: String = {
    description
  }
}


case class Field[+ValueType](
  name: String,
  values: Iterable[ValueType]=None,
  error: Option[FormError]=None
) extends Iterable[ValueType] {
  final def value = {
    values.headOption
  }

  def ifError(htmlGenerator: => Html): Html = {
    error.map(error => htmlGenerator).getOrElse(Html.empty)
  }

  def ifNotError(htmlGenerator: => Html): Html = {
    error.map(error => Html.empty).getOrElse(htmlGenerator)
  }

  //
  // Iterable[ValueType] members
  //
  def iterator: Iterator[ValueType] = {
    values.iterator
  }
}


object FieldFormatting {
  type WholeNumberFormattable = Any{ def toLong: Long }

  case class WholeNumberFormat[+T <: WholeNumberFormattable](field: T) {
    def toStringAsWholeNumber: String = {
      field.toLong.toString
    }
  }

  object Conversions {
    implicit def numberToWholeNumberFormat[T <: WholeNumberFormattable] (field: T): WholeNumberFormat[T] =
    {
      WholeNumberFormat(field)
    }
  }
}
