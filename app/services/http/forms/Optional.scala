package services.http.forms

trait Optional[+ValueType] { this: FormField[Option[ValueType]] =>
  protected def validateIfPresent: Either[FormError, ValueType]

  override final def validate= {
    if (stringsToValidate == null || stringsToValidate.isEmpty) {
      Right(None)
    } else {
      validateIfPresent match {
        case Left(someError) => Left(someError)
        case Right(actualValue) => Right(Some(actualValue))
      }
    }
  }
}
