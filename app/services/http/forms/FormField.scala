package services.http.forms

trait FormField[+ValueType] {
  //
  // Abstract public members
  //
  def name: String

  def stringsToValidate: Iterable[String]

  protected[forms] def validate: Either[FormError, ValueType]

  //
  // Public members
  //
  def value: Option[ValueType] = {
    validate.right.toOption
  }

  def error: Option[FormError] = {
    validate.left.toOption
  }

  def write(writeKeyValue: Form.Writeable) {
    writeKeyValue(name, stringsToValidate.mkString(","))
  }
}
