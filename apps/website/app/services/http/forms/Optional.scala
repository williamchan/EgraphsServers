package services.http.forms

/**
 * Helper [[services.http.forms.FormField]] trait that implements validate assuming
 * that the parameter is optional. Results in a FormField[Option[ValueType]]
 *
 * @tparam ValueType
 */
trait Optional[+ValueType] { this: FormField[Option[ValueType]] =>
  /**
   * Further validate stringsToValidate assuming that the parameter actually had values.
   */
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
