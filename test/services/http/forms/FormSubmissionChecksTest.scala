package services.http.forms

import utils.EgraphsUnitTest
import services.AppConfig
import java.text.SimpleDateFormat
import play.data.binding.types.DateBinder
import models.{AccountAuthenticationError, Account, AccountStore}

class FormSubmissionChecksTest extends EgraphsUnitTest {
  def check = AppConfig.instance[FormSubmissionChecks]

  "isInt" should "only accept integer strings and return them as Ints" in {
    check.isInt("1") should be (Right(1))
    check.isInt("100") should be (Right(100))
    check.isInt("100.10").isLeft should be (true)
    check.isInt("Herp").isLeft should be (true)
  }

  "isLong" should "only accept long strings and return them as Longs" in {
    check.isInt("1") should be (Right(1L))
    check.isInt("100") should be (Right(100L))
    check.isInt("100.10").isLeft should be (true)
    check.isInt("Herp").isLeft should be (true)
  }

  "isBoolean" should "only accept boolean strings and return them as Booleans" in {
    // Set up
    val trues  = List("1", "true",  "on",  "yes", "True",  "TRUE",  "YES", "Yes", " true ")
    val falses = List("0", "false", "off", "no" , "False", "FALSE", "NO" , " false ")

    // Run tests
    for (trueValue <- trues) check.isBoolean(trueValue) should be (Right(true))
    for (falseValue <- falses) check.isBoolean(falseValue) should be (Right(false))

    check.isBoolean("herp").isLeft should be (true)
  }

  "isDate" should "work with ISO8601 dates" in {
    // Set up
    val dateString = "ISO8601:2012-06-15T02:24:00-0800"
    val iso8601DateFormat = new SimpleDateFormat(DateBinder.ISO8601)

    // Run test
    check.isDate(dateString) should be (Right(iso8601DateFormat.parse(dateString)))
  }

  "isDateWithFormat" should "work with most dates" in {
    // Set up
    val formatString = "yyyy-MM-dd HH:mm:ss"
    val dateFormat = new SimpleDateFormat(formatString)
    val dateString = "2012-05-10 10:45:00"

    // Run tests
    check.isDateWithFormat(formatString, dateString) should be (
      Right(dateFormat.parse(dateString))
    )

    check.isDateWithFormat(formatString, "herp derp").isLeft should be (true)
  }

  "dependentFieldIsValid" should "return the field value if it's valid" in {
    // Set up
    val mockField = mock[FormField[Int]]
    val mockFieldResult = Right(100)
    mockField.validate returns mockFieldResult

    // Run tests
    check.dependentFieldIsValid(mockField) should be (mockFieldResult)
  }

  "dependentFieldIsValid" should "return DependentFieldError if the field wasn't valid" in {
    // Set up
    val mockField = mock[FormField[Int]]
    mockField.validate returns Left(new SimpleFormError("herp"))

    // Run tests
    check.dependentFieldIsValid(mockField).isLeft should be (true)
    check.dependentFieldIsValid(mockField).left.get.isInstanceOf[DependentFieldError] should be (true)
  }

  "isValidAccount" should "yield the valid account if it exists" in {
    // Set up
    val mockAccountStore = mock[AccountStore]
    val mockAccount = mock[Account]

    val email = "herp@derp.com"
    val pass = "derpityderp"

    mockAccountStore.authenticate(email, pass) returns Right(mockAccount)

    // Instantiate a new check instance that uses our mock
    val check = new FormSubmissionChecks(mockAccountStore)

    // Run tests
    check.isValidAccount(email, pass) should be (Right(mockAccount))

    // Set up to failed authentication and test again
    mockAccountStore.authenticate(email, pass) returns Left(mock[AccountAuthenticationError])
    check.isValidAccount(email, pass).isLeft should be (true)
  }

  "isCustomerAccount" should "yield the customer id of an account that has one" in {
    check.isCustomerAccount(Account(customerId=Some(1L))) should be (Right(1L))
    check.isCustomerAccount(Account(customerId=None)).isLeft should be (true)
  }

  "isEmailAddress" should "yield an email address if syntactically valid email otherwise an error" in {
    check.isEmailAddress("erem@egraphs.com") should be (Right("erem@egraphs.com"))
    check.isEmailAddress("Herp derpson").isLeft should be (true)
  }

  "isPhoneNumber" should "yield a phone number if syntactically valid otherwise an error" in {
    check.isPhoneNumber("650-223-4456") should be (Right("650-223-4456"))
    check.isPhoneNumber("herp derpson").isLeft should be (true)
  }

  "isPresent" should "yield a string array if it was non null and had at least one value" in {
    // Set up
    val passers: List[Iterable[String]] = List(
      Some("hello"),
      List("hello", "world"),
      List("", "herp"),
      List("herp", "")
    )

    val failers: List[Iterable[String]] = List(
      null,
      None,
      List(),
      List(""),
      List(null)
    )

    // Run tests
    for (passer <- passers) FormSubmissionChecks.isPresent(passer) should be (Right(passer))
    for (failer <- failers) FormSubmissionChecks.isPresent(failer).isLeft should be (true)
  }
}
