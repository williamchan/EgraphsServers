package services.http.forms

import play.api.mvc.Results.Redirect
import services.http.ServerSession
import services.http.forms.Form.FormWriteable
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.data.FormUtils
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import play.api.libs.json.JsNull
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.libs.json.JsValue
import play.api.libs.json.JsBoolean

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
  def redirectThroughFlash(url: String)(implicit flash: play.api.mvc.Flash): Result = {
    import Form.Conversions._

    val newFlash = this.write(flash.asFormWriteable).written
    Redirect(url).flashing(newFlash)
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
    override def stringsToValidate = {
      paramsMap(this.name)
    }

    /**
     * For convenience, the first value Option of stringsToValidate
     * @return
     */
    protected def stringToValidate: String = stringsToValidate.head

    // Adds the field to the form
    addField(this)
  }

  /**
   * Convenience method for specifying a field with less verbosity.
   * Usage {{{
   *   def check: FormChecks // gets assigned somehow...
   *
   *   val appleCount:Field[Int] = field("appleCount").validatedBy { paramValues =>
   *     for (
   *       param <- check.isSomeValue(paramValues.headOption).right;
   *       int <- check.isInt(param).right
   *     ) yield {
   *       int
   *     }
   *   }
   * }}}
   */
  protected def field[ValueType](paramName: String): FieldBuilder[ValueType] = {
    new FieldBuilder[ValueType](paramName)
  }

  /**
   * Allows you to build a field that is dependent upon other fields, rather than
   * being necessarily associate with a particular parameter value.
   *
   * See [[services.http.forms.CustomerLoginForm]] for usage.
   *
   */
  protected def field[ValueType]: FieldBuilder[ValueType] = {
    new FieldBuilder[ValueType]("")
  }

  /** Class used to help in convenience method `field`. See that method's documentation */
  protected class FieldBuilder[-ValueType](paramName: String) {

    def validatedBy[T <: ValueType](validation: (Iterable[String]) => Either[FormError, T])
    : Field[T] =
    {
      new Field[T] {
        val name = paramName
        val validate: Either[FormError, T] = {
          validation(stringsToValidate)
        }
      }
    }
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

  def write[T](formWriteable: FormWriteable[T]): FormWriteable[T] = {
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
  @Deprecated
  type Writeable = (String, Iterable[String]) => Unit


  /**
   * Interface for anything that can be written to from a Form subtype
   *
   * See [[services.http.forms.Form.ServerSessionWriteable]] for an example.
   *
   * @tparam T the type that is being written into
   */
  trait FormWriteable[T] {
    /**
     * Returns an instance of the type being written, with all previously written
     * tuples applied.
     */
    def written: T

    /**
     * Returns a new instance of the FormWriteable with the parameterized
     * pair written into it.
     * */
    def withData(toAdd:(String, Iterable[String])): FormWriteable[T]
  }

  /**
   * Structural type that allows any String-String puttable and gettable to turn into Form.Readable and
   * Form.Writeable
   */
  type StringGettablePuttable = { def get(key: String): String; def put(key: String, value: String) }

  type StringPuttable = { def put(key: String, value: String) }

  /**
   * FormWriteable that allows forms to write into any object that has a `def put(key: String, value: String)`
   * method. This includes most java map-like types including Play! scopes.
   */
  class MutableMapWriteable[T <: StringPuttable](val puttable: T) extends FormWriteable[T]
  {

    //
    // FormWriteable members
    //
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

  case class ImmutableMapWriteable[T <: { def +(kv: (String, String)): T }](appendable: T) extends FormWriteable[T]
  {
    // FormWriteable members
    val written: T = {
      appendable
    }

    def withData(toAdd: (String, Iterable[String])): ImmutableMapWriteable[T] = {
      val (key, rawValues) = toAdd

      // Replace instances of the serialization delimiter with something reasonable
      val escapedValues = rawValues.map(eachValue =>
        eachValue.replace(serializationDelimiter, "...")
      )

      val newAppendable = appendable + (key, escapedValues.mkString(serializationDelimiter))
      ImmutableMapWriteable(newAppendable)
    }
  }

  /**
   * FormWriteable that allows forms to write into a cache-backed ServerSession.
   */
  class ServerSessionWriteable(val written: ServerSession) extends FormWriteable[ServerSession] {
    def withData(toAdd: (String, Iterable[String])): ServerSessionWriteable = {
      new ServerSessionWriteable(written.setting(toAdd))
    }
  }

  /**
   * Implicit conversions for transforming some of our services into Form.Readable
   * and Form.Writeable instances.
   */
  object Conversions {
    //
    // Conversion classes
    //
    class FormCompatibleRequest(request: Request[_]) {
      def asFormReadable: Form.Readable = {
        val bodyMap = request.body match {
          case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
          case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined => body.asMultipartFormData.get.asFormUrlEncoded
          case body: play.api.mvc.AnyContent if body.asJson.isDefined => fromJson(js = body.asJson.get).mapValues(Seq(_))
          case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
          case body: play.api.mvc.MultipartFormData[_] => body.asFormUrlEncoded
          case body: play.api.libs.json.JsValue => fromJson(js = body).mapValues(Seq(_))
          case _ => Map.empty[String, Seq[String]]
        }
        
        val bodyAndQueryStringMap = bodyMap ++ request.queryString
        
        (key) => bodyAndQueryStringMap.get(key).getOrElse(Seq())
      }
      
      // This function was stolen bald-faced from Play's Form.scala, which chose
      //   not to make this function public. Really it should've just made a 
      //   non-type-safe version of the request as a form public...but how could
      //   they have imagined that we had an identical and parallel form API.
      private def fromJson(prefix: String = "", js: JsValue): Map[String, String] = js match {
        case JsObject(fields) => {
          fields.map { case (key, value) => fromJson(Option(prefix).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + key, value) }.foldLeft(Map.empty[String, String])(_ ++ _)
        }
        case JsArray(values) => {
          values.zipWithIndex.map { case (value, i) => fromJson(prefix + "[" + i + "]", value) }.foldLeft(Map.empty[String, String])(_ ++ _)
        }
        case JsNull => Map.empty
        case JsUndefined(_) => Map.empty
        case JsBoolean(value) => Map(prefix -> value.toString)
        case JsNumber(value) => Map(prefix -> value.toString)
        case JsString(value) => Map(prefix -> value.toString)
      }
    }

    class FormCompatiblePlayFlashAndSession[T <: { def get(key: String): Option[String]; def +(kv: (String, String)):T }](getabbleAppendable: T) {
      def asFormReadable: Form.Readable = {
        (key) => {
          val valueOption = getabbleAppendable.get(key)
          valueOption.map(valueString => valueString.split(serializationDelimiter)).toList.flatten
        }
      }

      def asFormWriteable: FormWriteable[T] = {
        ImmutableMapWriteable(getabbleAppendable)
      }
    }


    class FormCompatibleServerSession(serverSession: ServerSession) {
      def asFormReadable: Form.Readable = {
        (key) => serverSession[Iterable[String]](key).toList.flatten
      }

      def asFormWriteable: ServerSessionWriteable = {
        new ServerSessionWriteable(serverSession)
      }
    }

    implicit def playFlashOrSessionToFormCompatible[T <: { def get(key: String): Option[String]; def +(kv: (String, String)):T }](gettableAppendable: T)
    :FormCompatiblePlayFlashAndSession[T] =
    {
      new FormCompatiblePlayFlashAndSession(gettableAppendable)
    }

    //
    // Implicit conversions
    //
    implicit def playFlashOrSessionToMutableMapWriteable[T <: StringPuttable](puttable: T)
    : MutableMapWriteable[T] =
    {
      new MutableMapWriteable(puttable)
    }

    implicit def playRequestToFormCompatible(request: Request[_])
    : FormCompatibleRequest =
    {
      new FormCompatibleRequest(request)
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

  /** Separates serialized errors */
  private[Form] val errorSeparator = "…"
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
  private def formName = manifest.runtimeClass.getSimpleName
}
