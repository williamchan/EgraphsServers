package services.mvc

import utils.EgraphsUnitTest
import services.http.forms.{SimpleFormError, FormError, FormField}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FormConversionsTests extends EgraphsUnitTest {
  import FormConversions._

  "A Single string FormField's raw strings" should "be correctly converted into a view field" in {
    val theValue = "this is the value"
    val viewField = newFormField("field name", List(theValue)).asViewField

    viewField.name should be ("field name")
    viewField.error.toList should be (List())
    viewField.values.toList should be (List(theValue))
  }

  "A multiple-string FormField's raw strings" should "be correctly converted into a view field" in {
    val theValues = List("first", "second")
    val viewField = newFormField("field name", theValues).asViewField

    viewField.error.toList should be (List())
    viewField.values.toList should be (theValues.toList)
  }

  "A FormField's errors" should "be correctly converted into a view Field's errors" in {
    val errorMessage = "whoopsies!"
    val viewField = newFormField(
      "some field name",
      List("something field value"),
      Left(new SimpleFormError(errorMessage))
    ).asViewField
    viewField.error.map(_.description) should be (Some(errorMessage))
  }

  private def newFormField(
    fieldName: String,
    formValue: List[String],
    validateValue: Either[FormError, String] = Right("")
  ): FormField[String] =
  {
    new FormField[String] {
      val name = fieldName

      def stringsToValidate: Iterable[String] = formValue

      def validate = {
        validateValue
      }
    }
  }
}
