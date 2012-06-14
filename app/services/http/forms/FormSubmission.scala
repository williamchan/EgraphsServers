package services.http.forms

import play.mvc.results.Redirect

trait FormSubmission[+ValidFormType] {
  //
  // Abstract members
  //
  def paramsMap: FormSubmission.Readable
  protected def formAssumingValid: ValidFormType

  //
  // Public members
  //
  def errorsOrValidForm
  : Either[Iterable[FormError], ValidFormType] =
  {
    val errors = this.independentErrors

    if (errors.isEmpty) Right(formAssumingValid) else Left(errors)
  }

  def redirect(url: String)(implicit flash: play.mvc.Scope.Flash) {
    import FormSubmission.Conversions._

    this.write(flash.asSubmissionWriteable)
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

  private def write(writeKeyValue: FormSubmission.Writeable) {
    // Write all but the derived fields
    for (field <- fields if !field.isInstanceOf[DerivedField[_]]) field.write(writeKeyValue)
  }
}

object FormSubmission {
  type Readable = String => Iterable[String]
  type Writeable = (String, String) => Unit

  object Conversions {
    type HasGetAndPutString = { def get(key: String): String; def put(key: String, value: String) }

    class SubmissionCompatiblePlayParams(playParams: play.mvc.Scope.Params) {
      def asSubmissionReadable: FormSubmission.Readable = {
        (key) => playParams.getAll(key)
      }
    }

    class SubmissionCompatiblePlayFlashAndSession(gettablePuttable: HasGetAndPutString) {
      def asSubmissionReadable: FormSubmission.Readable = {
        (key) => Option(gettablePuttable.get(key))
      }

      def asSubmissionWriteable: FormSubmission.Writeable = {
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


