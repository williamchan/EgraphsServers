package services.http.forms

trait Required[+ValueType] { this: FormField[ValueType] =>
  protected def validateIfPresent: Either[FormError, ValueType]

  override final def validate = {
    for (strings <- FormChecks.isPresent(stringsToValidate).right;
         subclassResult <- validateIfPresent.right) yield subclassResult
  }
}
