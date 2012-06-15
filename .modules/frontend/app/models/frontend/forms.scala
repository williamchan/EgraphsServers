package models.frontend.forms


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
