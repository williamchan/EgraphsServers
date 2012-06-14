package services.http.forms

trait FormSubmission[+ValidFormType] {
  //
  // Abstract members
  //
  def paramsMap: FormSubmission.Readable
  protected def formAssumingValid: ValidFormType

  //
  // Public members
  //
  def errorsOrValidForm: Either[Iterable[FormError], ValidFormType] = {
    val errors = fields.foldLeft(Vector.empty[FormError]) { (accumulatedErrors, field) =>
      field.error match {
        case Some(error) => accumulatedErrors :+ error
        case None => accumulatedErrors
      }
    }

    if (errors.isEmpty) Right(formAssumingValid) else Left(errors)
  }

  def serializeToMap(writeKeyValue: FormSubmission.Writeable) {
    for (field <- fields) field.serializeToMap(writeKeyValue)
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

  //
  // Private members
  //
  private var fields = Vector[Field[_]]()

  private final def addField[T](newField: Field[T]) {
    fields = fields :+ newField
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

    implicit def playParamsToSubmissionCompatible(playParams: play.mvc.Scope.Params) = {
      new SubmissionCompatiblePlayParams(playParams)
    }

    implicit def playFlashOrSessionToSubmissionCompatible(gettablePuttable: HasGetAndPutString) = {
      new SubmissionCompatiblePlayFlashAndSession(gettablePuttable)
    }
  }
}