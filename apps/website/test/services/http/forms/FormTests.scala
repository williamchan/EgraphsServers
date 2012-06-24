package services.http.forms

import utils.{FunctionalTestUtils, EgraphsUnitTest}
import services.AppConfig
import play.test.FunctionalTest
import play.mvc.Scope
import Form.Conversions._

class FormTests extends EgraphsUnitTest {
  import TestPersonForm.Fields

  "Required parameters" should "be required" in {
    val paramSetsWithoutEmail: Iterable[Map[String, Iterable[String]]] = List(
      Map(Fields.Email -> List()),
      Map(Fields.Email -> null),
      Map(Fields.Email -> None),
      Map(Fields.Email -> List(""))
    )

    for (bogusParams <- paramSetsWithoutEmail) {
      newForm(formReadable(bogusParams)).email.error match {
        case Some(_: ValueNotPresentFieldError) =>
        case somethingElse =>
          fail("" + somethingElse + " should have been a ValueNotPresentFieldError")
      }
    }
  }

  "Required parameters" should "be processed if present" in {
    val paramSetsWithEmail: Iterable[Map[String, Iterable[String]]] = List(
      Map(Fields.Email -> List("herp")),
      Map(Fields.Email -> List("herp", "derp"))
    )

    for (nonEmptyParams <- paramSetsWithEmail) {
      newForm(formReadable(nonEmptyParams)).email.error match {
        case Some(notPresent: ValueNotPresentFieldError) =>
          fail("" + notPresent + " should have been present")
        case _ =>
      }
    }
  }

  "Optional parameters" should "not be required" in {
    val paramSetsWithoutAge: Iterable[Map[String, Iterable[String]]] = List(
      Map(Fields.Age -> List()),
      Map(Fields.Age -> null),
      Map(Fields.Age -> None),
      Map(Fields.Age -> List(""))
    )
    
    for (emptyParams <- paramSetsWithoutAge) {
      newForm(formReadable(emptyParams)).age.value match {
        case Some(None) =>
        case _ => fail("Some(None) should have been the value of age")
      }
    }
  }

  "Optional parameters" should "be Some(value) when present" in {
    val form = newForm(formReadable(Map(Fields.Age -> List("22"))))
    form.age.value should be (Some(Some(22)))
  }

  "Form.errorsOrValidatedForm" should "return Left(all errors) if there were errors" in {
    val form = newForm(formReadable(
      Map(Fields.Email -> Some("herp"), Fields.Name -> Some("Herp Derpson"), Fields.Age -> Some("20"))
    ))
    val errorsOrValid = form.errorsOrValidatedForm
    val errorStringsOrValid = errorsOrValid.left.map(errors => errors.map(error => error.description))
    errorStringsOrValid.isLeft should be (true)

    val errorStrings = errorStringsOrValid.left.get
    errorStrings.size should be (2)

    val fullErrorString = errorStrings.mkString(", ")
    fullErrorString.contains("Valid email address required") should be (true)
    fullErrorString.contains("Under minimum value") should be (true)
  }

  "Form.errorsOrValidatedForm" should "return Right(ValidFormType) if there were no errors" in {
    val errorsOrValidatedForm = newValidForm.errorsOrValidatedForm

    errorsOrValidatedForm.isRight should be (true)
    errorsOrValidatedForm should be (
      Right(TestPersonForm.Valid("herp@derp.com", Some("Herp Derpson"), Some(22), Some(List("Apples", "Oranges"))))
    )
  }

  "Form.addError" should "append a field-inspecific error" in {
    // Set up
    val form = newValidForm
    val error = new SimpleFormError("Herp did not derp")

    // Run test
    form.fieldInspecificErrors.size should be (0)
    form.addError(error)
    form.fieldInspecificErrors should be (Vector(error))
  }

  "Writing/reading" should "save and load form-inspecific errors and all optional/required fields" in {
    // Set up
    val flash = new Scope.Flash

    val originalForm = newValidForm
    val formErrorString = "This form had an error unrelated to any particular field"
    originalForm.addError(new SimpleFormError(formErrorString))

    val reader = new TestPersonFormFactory

    originalForm.write(flash.asFormWriteable)
    val recoveredFormOption = reader.read(flash.asFormReadable)

    // Check expectations
    recoveredFormOption match {
      case Some(recoveredForm) =>
        recoveredForm.age.stringsToValidate should be (originalForm.age.stringsToValidate)
        recoveredForm.email.stringsToValidate should be (originalForm.email.stringsToValidate)
        recoveredForm.name.stringsToValidate should be (originalForm.name.stringsToValidate)
        recoveredForm.favoriteFruits.stringsToValidate should be (originalForm.favoriteFruits.stringsToValidate)

        val errorString = recoveredForm.fieldInspecificErrors.map(error => error.description).mkString
        errorString.contains(formErrorString) should be (true)

      case None =>
        fail("A form should have been recovered from the flash")
    }
  }

  "redirectThroughFlash" should "save the form in the flash and return a redirect to the correct URL" in {
    // Set up
    implicit val flash = new Scope.Flash
    val redirect = newValidForm.redirectThroughFlash("/herp")

    // Check expectations
    redirect.url should be ("/herp")
    new TestPersonFormFactory().read(flash.asFormReadable).isDefined should be (true)
  }

  "Conversions" should "correctly convert a flash into a readable" in {
    // Set up
    val flash = new Scope.Flash
    flash.put("null", null)
    flash.put("empty", "")
    flash.put("value", "value")

    val read = flash.asFormReadable

    // Check expectations
    read("null").toList should be (List())
    read("empty").toList should be (List(""))
    read("value").toList should be (List("value"))
  }

  "Conversions" should "correctly convert a Scope.Flash into a writeable" in {
    // Set up
    val flash = new Scope.Flash
    val writeable = flash.asFormWriteable
    val read = flash.asFormReadable

    writeable
      .withData("single value" -> List("herp"))
      .withData("multiple value" -> List("herp", "derp"))

    // Check expectations
    read("null").toList should be (List())
    read("single value").toList should be (List("herp"))
    read("multiple value").toList should be (List("herp", "derp"))
  }

  "Conversions" should "correctly convert a Scope.Params into a readable" in {
    // Set up
    val params = FunctionalTest.newRequest().params
    params.put("empty", Array(""))
    params.put("one value", Array("value"))
    params.put("multiple values", Array("first", "second"))

    val read = params.asFormReadable

    // Check expectations
    read("null").toList should be (List())
    read("empty").toList should be (List(""))
    read("one value").toList should be (List("value"))
    read("multiple values").toList should be (List("first", "second"))
  }


  //
  // A sample form class
  //
  class TestPersonForm(val paramsMap: Form.Readable, check: FormChecks)
    extends Form[TestPersonForm.Valid]
  {
    import TestPersonForm.Fields

    val email = new RequiredField[String](Fields.Email) {
      def validateIfPresent = {
        for (validEmail <- check.isEmailAddress(stringToValidate).right) yield validEmail
      }
    }

    val name = new OptionalField[String](Fields.Name) {
      def validateIfPresent = {
        Right(stringToValidate)
      }
    }

    val age = new OptionalField[Int](Fields.Age) {
      def validateIfPresent = {
        for (validInt <- check.isInt(stringToValidate).right;
             legalAge <- check.isAtLeast(21, validInt).right)
        yield {
          legalAge
        }
      }
    }

    val favoriteFruits = new OptionalField[List[String]](Fields.Fruits) {
      def validateIfPresent = {
        Right(stringsToValidate.toList)
      }
    }

    protected def formAssumingValid = {
      TestPersonForm.Valid(
        email.value.get,
        name.value.get,
        age.value.get,
        favoriteFruits.value.get
      )
    }
  }


  object TestPersonForm {
    object Fields {
      val Name = "person.name"
      val Email = "person.email"
      val Age = "person.age"
      val Fruits = "person.favoriteFruits"
    }

    case class Valid(email: String, name: Option[String], age: Option[Int], fruits: Option[List[String]])
  }


  class TestPersonFormFactory extends ReadsForm[TestPersonForm] {
    def instantiateAgainstReadable(readable: Form.Readable): TestPersonForm = {
      new TestPersonForm(readable, AppConfig.instance[FormChecks])
    }
  }

  //
  // Private helpers
  //
  private def formReadable(map: Map[String, Iterable[String]]): Form.Readable = {
    (key: String) => map.get(key).getOrElse(List())
  }

  private def newForm(readable: Form.Readable): TestPersonForm = {
    new TestPersonForm(readable, AppConfig.instance[FormChecks])
  }

  private def newValidForm = {
    newForm(formReadable(Map(
      Fields.Email -> Some("herp@derp.com"),
      Fields.Name -> Some("Herp Derpson"),
      Fields.Age -> Some("22"),
      Fields.Fruits -> List("Apples", "Oranges")
    )))
  }
}
