package services.http.forms

import services.http.forms.Form.FormWriteable

/**
 * Base definition of a Field as implemented in [[services.http.forms.Form]]
 *
 * @tparam ValueType the type of value represented by the field
 */
trait FormField[+ValueType] {
  //
  // Abstract public members
  //
  /**
   * The field's name in the provided form
   */
  def name: String

  /**
   * The strings that represent this field in the provided parameter map, be
   * it request params, flash, or session
   */
  def stringsToValidate: Iterable[String]

  /**
   * Perform validation of stringsToValidate in this implementation. It's easiest
   * to use an instance of [[services.http.forms.FormChecks]]. See [[services.http.forms.Form]]
   * for more info.
   *
   * @return the result of validating `stringsToValidate`
   */
  protected[forms] def validate: Either[FormError, ValueType]

  //
  // Public members
  //
  /**
   * The form value if the field was valid. Otherwise None.
   */
  def value: Option[ValueType] = {
    validate.right.toOption
  }

  /**
   * The validation errors, if there were any. Otherwise None.
   */
  def error: Option[FormError] = {
    validate.left.toOption
  }

  def independentError: Option[FormError] = {
    validate.left.toOption.filter(error => !error.isInstanceOf[DependentFieldError])
  }

  //
  // Private members
  //
  @Deprecated
  private[forms]def write(writeKeyValue: Form.Writeable) {
    writeKeyValue(name, stringsToValidate)
  }

  private[forms] def write[T](writeable: FormWriteable[T]): FormWriteable[T] = {
    if (!stringsToValidate.isEmpty && stringsToValidate.head != "") {
      writeable.withData(name -> stringsToValidate)
    } else {
      writeable
    }
  }
}
