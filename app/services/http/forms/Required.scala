package services.http.forms

trait Required[+ValueType] { this: FormField[ValueType] =>
  protected def validateIfPresent: Either[FormError, ValueType]

  override final protected def validation = {
    if (stringsToValidate == null || stringsToValidate.isEmpty) {
      Left(ValueNotPresentFieldError())
    } else {
      validateIfPresent
    }
  }
}
