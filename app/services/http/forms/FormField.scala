package services.http.forms

trait FormField[+ValueType] {
  //
  // Abstract public members
  //
  def name: String
  def stringsToValidate: Iterable[String]

  protected def validation: Either[FormError, ValueType]

  //
  // Public members
  //
  def value: Option[ValueType] = {
    validation.right.toOption
  }

  def error: Option[FormError] = {
    validation.left.toOption
  }

  def serializeToMap(writeKeyValue: FormSubmission.Writeable) {
    writeKeyValue(name, stringsToValidate.mkString(","))
  }
}
