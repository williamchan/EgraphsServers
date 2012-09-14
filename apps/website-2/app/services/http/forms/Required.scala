package services.http.forms

/**
 * Helper [[services.http.forms.FormField]] trait that implements validate assuming
 * that the parameter is required. Results in a FormField[ValueType]
 *
 * @tparam ValueType the type of value this form represents. For example, if the form
 *   had an `age` parameter this type would be Required[Int]
 */
trait Required[+ValueType] { this: FormField[ValueType] =>
  /**
   * Further validate stringsToValidate assuming that the parameter actually had values.
   */
  protected def validateIfPresent: Either[FormError, ValueType]

  /**
   * Message of FormError if required value is missing.
   */
  def errorMessage: String = "Required"

  override final def validate = {
    for (strings <- FormChecks.isPresent(stringsToValidate, errorMessage).right;
         subclassResult <- validateIfPresent.right) yield subclassResult
  }
}
