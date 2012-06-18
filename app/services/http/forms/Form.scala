package services.http.forms

import play.mvc.results.Redirect

trait Form[+ValidFormType] {
  //
  // Abstract members
  //
  protected def paramsMap: Form.Readable
  protected def formAssumingValid: ValidFormType

  //
  // Public members
  //
  def errorsOrValidatedForm: Either[Iterable[FormError], ValidFormType] = {
    val errors = this.independentErrors

    if (errors.isEmpty) Right(formAssumingValid) else Left(errors)
  }

  def addError(error: FormError) {
    _fieldInspecificErrors = _fieldInspecificErrors :+ error
  }

  def fieldInspecificErrors: Iterable[FormError] = {
    val maybeStoredErrors = for (errorsString <- paramsMap(serializedErrorsKey).headOption) yield {
      for (singleErrorString <- errorsString.split(Form.errorSeparator) if singleErrorString != "")
      yield {
        new SimpleFormError(singleErrorString)
      }
    }

    _fieldInspecificErrors ++ maybeStoredErrors.getOrElse(Array()) ++ derivedErrors
  }

  def redirectThroughFlash(url: String)(implicit flash: play.mvc.Scope.Flash): Redirect = {
    import Form.Conversions._

    this.write(flash.asFormWriteable)
    new Redirect(url)
  }

  //
  // Protected Members
  //
  protected abstract class Field[+T] extends FormField[T]  {
    override def name: String
    override val stringsToValidate  = paramsMap(this.name)
    protected def stringToValidate: String = stringsToValidate.head

    addField(this)
  }

  protected abstract class RequiredField[+ValueType](val name: String) extends Field[ValueType]
    with Required[ValueType]

  protected abstract class OptionalField[+ValueType](val name: String) extends Field[Option[ValueType]]
    with Optional[ValueType]

  protected abstract class DerivedField[+ValueType] extends Field[ValueType] {
    override def name: String = ""
  }

  //
  // Private members
  //
  protected val formName = this.getClass.getSimpleName
  private val serializedErrorsKey = formName + ".errors"
  private var fields = Vector[Field[_]]()
  private var _fieldInspecificErrors = Vector[FormError]()

  private def independentErrors:Iterable[FormError] = {
    fields.foldLeft(Vector.empty[FormError]) { (accumulatedErrors, field) =>
      field.error match {
        case None | Some(_:DependentFieldError) => accumulatedErrors
        case Some(independentError) => accumulatedErrors :+ independentError
      }
    }
  }

  private def derivedErrors: Iterable[FormError] = {
    for (field <- fields if field.isInstanceOf[DerivedField[_]];
         error <- field.error if !error.isInstanceOf[DependentFieldError])
    yield {
      error
    }
  }

  private final def addField[T](newField: Field[T]) {
    fields = fields :+ newField
  }

  private[forms] def write(writeKeyValue: Form.Writeable) {
    // Write the form name
    writeKeyValue(formName, Some("true"))

    // Write all but the derived fields
    for (field <- fields if !field.isInstanceOf[DerivedField[_]]) field.write(writeKeyValue)

    // Write the field-inspecific errors
    val errorString = _fieldInspecificErrors.map(error => error.description).mkString(Form.errorSeparator)
    writeKeyValue(serializedErrorsKey, Some(errorString))
  }
}


object Form {
  val errorSeparator = "…"

  type Readable = String => Iterable[String]
  type Writeable = (String, Iterable[String]) => Unit

  object Conversions {
    type HasGetAndPutString = { def get(key: String): String; def put(key: String, value: String) }


    class FormCompatiblePlayParams(playParams: play.mvc.Scope.Params) {
      def asFormReadable: Form.Readable = {
        (key) => Option(playParams.getAll(key)).flatten
      }
    }

    class FormCompatiblePlayFlashAndSession(gettablePuttable: HasGetAndPutString) {
      def asFormReadable: Form.Readable = {
        (key) => {
          val valueOption = Option(gettablePuttable.get(key))
          valueOption.map(valueString => valueString.split(delimiter)).flatten
        }
      }

      def asFormWriteable: Form.Writeable = {
        (key, values) => gettablePuttable.put(key, values.mkString(delimiter))
      }

      private def delimiter = ",,"
    }

    implicit def playFlashOrSessionToFormCompatible(gettablePuttable: HasGetAndPutString)
    :FormCompatiblePlayFlashAndSession =
    {
      new FormCompatiblePlayFlashAndSession(gettablePuttable)
    }

    implicit def playParamsToFormCompatible(playParams: play.mvc.Scope.Params)
    : FormCompatiblePlayParams =
    {
      new FormCompatiblePlayParams(playParams)
    }
  }
}


abstract class ReadsForm[+FormType <: Form[_]](implicit manifest: Manifest[FormType])
{
  //
  // Abstract members
  //
  def instantiateAgainstReadable(readable: Form.Readable): FormType

  //
  // Public members
  //
  private def formName = manifest.erasure.getSimpleName

  def read(readable: Form.Readable): Option[FormType] =
  {
    if (!readable(formName).isEmpty) {
      Some(instantiateAgainstReadable(readable))
    } else {
      None
    }
  }

}
