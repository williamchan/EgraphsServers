package services.http.forms

import play.mvc.results.Redirect
import play.mvc.Scope.Session
import services.http.ServerSession
import services.http.forms.Form.FormWriteable

/**
 * Abstraction of an HTTP form. Extend and use it to abstract parameter validation from domain logic.
 * See [[services.http.forms.CustomerLoginForm]] and [[services.http.forms.CustomerLoginFormFactory]]
 * for good examples.
 *
 * @tparam ValidFormType the domain type that the field resolves to. For example, although the submitted
 *     fields of a CustomerLoginForm are `email` and `password`, semantically what we are actually
 *     trying to log in is a customer ID. This is why [[services.http.forms.CustomerLoginForm.Validated]]
 *     only has one field of type `Long`
 */
trait Form[+ValidFormType] {
  //
  // Abstract members
  //
  /**
   * The String -> Iterable[String] map that gives the form values. You should generally
   * use Form.Conversions._ to automatically convert Play params and flash scopes into
   * this type.
   */
  protected def paramsMap: Form.Readable

  /**
   * Provides the mapping from FormFields to your ValidFormType.
   *
   * See [[services.http.forms.CustomerLoginForm]] for a good example.
   *
   * @return an instance of your ValidFormType.
   */
  protected def formAssumingValid: ValidFormType

  //
  // Public members
  //
  /**
   * Called by a POST controller to process whether the form was valid or not. See
   * the PostLoginEndpoint controller trait for an example.
   *
   * Usage:
   * {{{
   *   def myController = postControllerMethod() {
   *     val myForm = myFormFactory(request.params.asFormReadable)
   *     myForm.errorsOrValidatedForm match {
   *       case Left(errors) =>
   *          // Handle the errors
   *
   *       case Right(validatedForm) =>
   *          // Use the instance of ValidFormType to do some work.
   *     }
   *   }
   *
   * }}}
   *
   * @return
   */
  def errorsOrValidatedForm: Either[Iterable[FormError], ValidFormType] = {
    val errors = this.independentErrors

    if (errors.isEmpty) Right(formAssumingValid) else Left(errors)
  }

  /**
   * Adds errors identified after field validation. You should usually not use this, and should
   * *never* use it for field validation. Pretty much exclusively use it after delegating
   * most form validation to `myForm.errorsOrValidatedForm`
   *
   * @param error the error to add
   */
  def addError(error: FormError) {
    _fieldInspecificErrors = _fieldInspecificErrors :+ error
  }

  /**
   * Returns all errors that were not directly associated with raw input fields.
   *
   * These are any errors added with `addError`, discovered in the parameter map via
   * `paramsMap(serializedErrorsKey)`, or generated by a `DerivedField`.
   */
  def fieldInspecificErrors: Iterable[FormError] = {
    val maybeStoredErrors = for (errorsString <- paramsMap(serializedErrorsKey).headOption) yield {
      for (singleErrorString <- errorsString.split(Form.errorSeparator) if singleErrorString != "")
      yield {
        new SimpleFormError(singleErrorString)
      }
    }

    _fieldInspecificErrors ++ maybeStoredErrors.getOrElse(Array()) ++ derivedErrors
  }

  /**
   * Serializes the form into the Flash scope and returns a redirect to the specified
   * URL.
   *
   * @param url the url to redirect to
   * @param flash the current flash scope into which the form should be saved.
   * @return a Redirect result for Play to process
   */
  def redirectThroughFlash(url: String)(implicit flash: play.mvc.Scope.Flash): Redirect = {
    import Form.Conversions._

    this.write(flash.asFormWriteable)
    new Redirect(url)
  }

  //
  // Protected Members
  //
  /**
   * Base class for specifying field names and handling their validation. It does no
   * pre-validation on whether a required field is there or not, so it may be preferable
   * to the more generally useful RequiredField and OptionalFields in cases where the
   * *absence* of the field is meaningful (e.g. check boxes)
   *
   * Usage:
   * {{{
   *   val isChecked = new Field[Boolean] {
   *     def name = "isChecked"
   *     def validate = {
   *       Right(stringsToValidate.headOption.map(_ => true).getOrElse(false))
   *     }
   *   }
   * }}}
   *
   * See Form usage for more examples.
   */
  protected abstract class Field[+ValueType] extends FormField[ValueType]  {
    /**
     * The key for the field in the paramsMap. This should match up with the body parameter from
     * the HTTP form.
     * @return
     */
    override def name: String

    /**
     * The string parameters that this field is responsible for validationg
     */
    override val stringsToValidate = paramsMap(this.name)

    /**
     * For convenience, the first value Option of stringsToValidate
     * @return
     */
    protected def stringToValidate: String = stringsToValidate.head

    // Adds the field to the form
    addField(this)
  }

  /**
   * Convenience Field to specify that the field is required. You must only implement validateIfPresent, and can
   * assume that there is at least one non-empty-string, non-null value for the parameter.
   *
   * See [[services.http.forms.CustomerLoginForm]] for a good example.
   *
   * @param name see Field.name
   * @tparam ValueType the type of the field
   */
  protected abstract class RequiredField[+ValueType](val name: String) extends Field[ValueType]
    with Required[ValueType]

  /**
   * Convenience Field to specify that the field is optional. You must only implement validateIfPresent,
   * and can assume while implementing that there was at least one non-empty-string, non-null value for
   * the parameter.
   *
   * Its `validate` implementation automatically returns None in any other scenario.
   *
   * @param name see Field.name
   * @tparam ValueType see Field.valueType
   */
  protected abstract class OptionalField[+ValueType](val name: String) extends Field[Option[ValueType]]
    with Optional[ValueType]


  /**
   * Convenience Field to specify that the field was not submitted, but is derived from other
   * submitted fields. For this reason it has no `name` field and never gets serialized.
   *
   * The `validate` methods on these kinds of fields should almost always start with calls
   * to [[services.http.forms.FormChecks.dependentFieldIsValid()]]
   *
   * @tparam ValueType see Field.valueType
   */
  protected abstract class DerivedField[+ValueType] extends Field[ValueType] {
    override final def name: String = ""
  }

  //
  // Private members
  //

  // name of the form for use in telling if the form has been serialized into the paramsMap
  protected val formName = this.getClass.getSimpleName

  // name of the form for grabbing previously serialized errors from the paramsMap
  private val serializedErrorsKey = formName + ".errors"

  // The list of fields, automatically populated by extending this.Field
  private var fields = Vector[Field[_]]()

  // Errors added by this.addFormError
  private var _fieldInspecificErrors = Vector[FormError]()

  // Errors in any non-derived fields (Required or Optional)
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

  // Adds a field to the form. This is automatically called by any subclass of this.Field
  private final def addField[T](newField: Field[T]) {
    fields = fields :+ newField
  }

  // Serializes the form out to a [[Form.Writeable]]. Usually this will be a flash or session scope
  // converted using Form.Conversions._
  private[forms] def write(writeKeyValue: Form.Writeable) {
    // Write the form name
    writeKeyValue(formName, Some("true"))

    // Write all but the derived fields
    for (field <- fields if !field.isInstanceOf[DerivedField[_]]) field.write(writeKeyValue)

    // Write the field-inspecific errors
    val errorString = _fieldInspecificErrors.map(error => error.description).mkString(Form.errorSeparator)
    writeKeyValue(serializedErrorsKey, Some(errorString))
  }

  private[forms] def write[T](formWriteable: FormWriteable[T]): FormWriteable[T] = {
    // Write all the submitted fields
    val submittedFields = fields.filter(field => !field.isInstanceOf[DerivedField[_]])
    val submittedFieldsWritten = submittedFields.foldLeft(formWriteable)(
      (writeable, nextField) => nextField.write(writeable)
    )

    // Write form name and errors
    val errorString = _fieldInspecificErrors.map(error => error.description).mkString(Form.errorSeparator)
    submittedFieldsWritten
      .withData(formName -> Some("true"))
      .withData(serializedErrorsKey -> Some(errorString))
  }
}


object Form {

  /** Interface for anything that can be read as a paramsMap for a Form subtype */
  type Readable = String => Iterable[String]

  /** Interface for anything that can be written to from a Form subtype */
  type Writeable = (String, Iterable[String]) => Unit

  trait FormWriteable[T] {
    def written: T
    def withData(toAdd:(String, Iterable[String])): FormWriteable[T]
  }

  /** Separates serialized errors */
  private[Form] val errorSeparator = "…"

  /**
   * Structural type that allows any String-String puttable and gettable to turn into Form.Readable and
   * Form.Writeable
   */
  type StringGettablePuttable = { def get(key: String): String; def put(key: String, value: String) }

  type StringPuttable = { def put(key: String, value: String) }

  class MutableMapWriteable[T <: StringPuttable](val puttable: T) extends FormWriteable[T]
  {

    val written: T = {
      puttable
    }

    def withData(toAdd: (String, Iterable[String])): MutableMapWriteable[T] = {
      val (key, rawValues) = toAdd

      // Replace instances of the serialization delimiter with something reasonable
      val escapedValues = rawValues.map(eachValue =>
        eachValue.replace(serializationDelimiter, "...")
      )

      puttable.put(key, escapedValues.mkString(serializationDelimiter))

      this
    }
  }

  class ServerSessionWriteable(val written: ServerSession) extends FormWriteable[ServerSession] {
    def withData(toAdd: (String, Iterable[String])): ServerSessionWriteable = {
      new ServerSessionWriteable(written.setting(toAdd))
    }
  }

  /**
   * Implicit conversions for transforming request- and session-specific values into Form.Readable
   * and Form.Writeable instances.
   */
  object Conversions {
    //
    // Conversion classes
    //
    class FormCompatiblePlayParams(playParams: play.mvc.Scope.Params) {
      def asFormReadable: Form.Readable = {
        (key) => Option(playParams.getAll(key)).flatten
      }
    }

    class FormCompatiblePlayFlashAndSession[T <: StringGettablePuttable](gettablePuttable: T) {
      def asFormReadable: Form.Readable = {
        (key) => {
          val valueOption = Option(gettablePuttable.get(key))
          valueOption.map(valueString => valueString.split(serializationDelimiter)).flatten
        }
      }

      def asFormWriteable: FormWriteable[T] = {
        new MutableMapWriteable(gettablePuttable)
      }
    }

    class FormCompatibleServerSession(serverSession: ServerSession) {
      def asFormReadable: Form.Readable = {
        (key) => serverSession[Iterable[String]](key).flatten
      }

      def asFormWriteable: ServerSessionWriteable = {
        new ServerSessionWriteable(serverSession)
      }
    }

    implicit def playFlashOrSessionToFormCompatible[T <: StringGettablePuttable](gettablePuttable: T)
    :FormCompatiblePlayFlashAndSession[T] =
    {
      new FormCompatiblePlayFlashAndSession(gettablePuttable)
    }

    //
    // Implicit conversions
    //
    implicit def playFlashOrSessionToMutableMapWriteable[T <: StringPuttable](puttable: T)
    : MutableMapWriteable[T] =
    {
      new MutableMapWriteable(puttable)
    }

    implicit def playParamsToFormCompatible(playParams: play.mvc.Scope.Params)
    : FormCompatiblePlayParams =
    {
      new FormCompatiblePlayParams(playParams)
    }

    implicit def serverSessionToFormCompatible(serverSession: ServerSession)
    : FormCompatibleServerSession =
    {
      new FormCompatibleServerSession(serverSession)
    }
  }

  //
  // Private members
  //
  private val serializationDelimiter = ",,,"
}


/**
 * Class that helps read Forms from a Form.Readable. You should usually do this from a
 * factory class.
 *
 * Usage:
 * {{{
 *   class MyFormFactory @Inject()(checks: FormChecks) extends ReadsForm[MyForm] {
 *     val instantiateAgainstReadable(readable: Form.Readable): MyForm = {
 *       new MyForm(readable, checks)
 *     }
 *   }
 * }}}
 *
 */
abstract class ReadsForm[+FormType <: Form[_]](implicit manifest: Manifest[FormType])
{
  //
  // Abstract members
  //
  /**
   * Create a new instance of your form against a Form.Readable. See
   * [[services.http.forms.CustomerLoginForm]] for a good example.
   *
   * @param readable the readable to instantiate against
   * @return your instance.
   */
  def instantiateAgainstReadable(readable: Form.Readable): FormType

  //
  // Public members
  //
  def read(readable: Form.Readable): Option[FormType] = {
    if (!readable(formName).isEmpty) {
      Some(instantiateAgainstReadable(readable))
    } else {
      None
    }
  }

  //
  // Private members
  //
  /** Name as the form as specified in the manifest */
  private def formName = manifest.erasure.getSimpleName
}
