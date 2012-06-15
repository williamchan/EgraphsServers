package services.http.forms

import play.mvc.results.Redirect

trait Form[+ValidFormType] {
  //
  // Abstract members
  //
  def paramsMap: Form.Readable
  protected def formAssumingValid: ValidFormType

  //
  // Public members
  //
  def errorsOrValidatedForm
  : Either[Iterable[FormError], ValidFormType] =
  {
    val errors = this.independentErrors

    if (errors.isEmpty) Right(formAssumingValid) else Left(errors)
  }

  def flashRedirect(url: String)(implicit flash: play.mvc.Scope.Flash) {
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
  private var fields = Vector[Field[_]]()

  private def independentErrors:Iterable[FormError] = {
    fields.foldLeft(Vector.empty[FormError]) { (accumulatedErrors, field) =>
      field.error match {
        case None | Some(_:DependentFieldError) => accumulatedErrors
        case Some(independentError) => accumulatedErrors :+ independentError
      }
    }
  }

  private final def addField[T](newField: Field[T]) {
    fields = fields :+ newField
  }

  private def write(writeKeyValue: Form.Writeable) {
    // Write the form name
    writeKeyValue(formName, "true")

    // Write all but the derived fields
    for (field <- fields if !field.isInstanceOf[DerivedField[_]]) field.write(writeKeyValue)
  }

  protected val formName = this.getClass.getName
}

object Form {
  type Readable = String => Iterable[String]
  type Writeable = (String, String) => Unit

  object Conversions {
    type HasGetAndPutString = { def get(key: String): String; def put(key: String, value: String) }

    class SubmissionCompatiblePlayParams(playParams: play.mvc.Scope.Params) {
      def asFormReadable: Form.Readable = {
        (key) => playParams.getAll(key)
      }
    }

    class SubmissionCompatiblePlayFlashAndSession(gettablePuttable: HasGetAndPutString) {
      def asFormReadable: Form.Readable = {
        (key) => Option(gettablePuttable.get(key))
      }

      def asFormWriteable: Form.Writeable = {
        (key, value) => gettablePuttable.put(key, value)
      }
    }

    implicit def playFlashOrSessionToSubmissionCompatible(gettablePuttable: HasGetAndPutString)
    :SubmissionCompatiblePlayFlashAndSession =
    {
      new SubmissionCompatiblePlayFlashAndSession(gettablePuttable)
    }

    implicit def playParamsToSubmissionCompatible(playParams: play.mvc.Scope.Params)
    : SubmissionCompatiblePlayParams =
    {
      new SubmissionCompatiblePlayParams(playParams)
    }
  }

}


abstract class ReadsFormSubmission[+SubmissionType <: Form[_]]
  (implicit manifest: Manifest[SubmissionType])
{
  //
  // Abstract members
  //
  def instantiateAgainstReadable(readable: Form.Readable): SubmissionType

  //
  // Public members
  //
  private def formName = manifest.erasure.getName

  def read(readable: Form.Readable, formChecks: FormSubmissionChecks)
  : Option[SubmissionType] =
  {
    if (!readable(formName).isEmpty) {
      Some(instantiateAgainstReadable(readable))
    } else {
      None
    }
  }

}
