package services.http.forms

trait Optional[+ValueType] { this: FormField[Option[ValueType]] =>
  protected def validateIfPresent: Either[FormError, ValueType]

  override final def validate = {
    FormChecks.isPresent(stringsToValidate) match {
      case Left(_) =>
        Right(None)

      case Right(realStrings) =>
        for (value <- validateIfPresent.right) yield Some(value)
    }
  }
}
