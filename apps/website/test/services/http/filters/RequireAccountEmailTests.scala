package services.http.filters

import org.junit.runner.RunWith
import models.Account
import models.AccountStore
import play.api.data.Form
import play.api.mvc.Results.Ok
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest
import org.scalatest.junit.JUnitRunner
import services.http.filters.FilterTestUtil._

@RunWith(classOf[JUnitRunner])
class RequireAccountEmailTests extends EgraphsUnitTest {
  val emailWithAccount = "myyk.seok@gmail.com"
  val emailWithoutAccount = "joe.phoney@gmail.com"

  val validAccount = Account(email = emailWithAccount, services = null)

  "filter" should "allow emails that have an account associated with them" in {
    val errorOrAccount = filterWithMocks.filter(emailWithAccount)

    errorOrAccount should be(Right(validAccount))
  }

  it should "not allow emails that do not have an account associated with them" in {
    val errorOrAccount = filterWithMocks.filter(emailWithoutAccount)

    val result = errorOrAccount.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  "form" should "require an email" in {
    val errorsOrEmail = filterWithMocks.form.bind(Map("email" -> emailWithAccount))

    errorsOrEmail.value should be(Some(emailWithAccount))
  }

  it should "fail if it does not have an email" in {
    val errorsOrEmail = filterWithMocks.form.bind(Map.empty[String, String])

    errorsOrEmail.value should be(None)
  }

  it should "fail if it has an invalid email string" in {
    val errorsOrEmail = filterWithMocks.form.bind(Map("email" -> "thisIsNotAnEmailFormat"))

    errorsOrEmail.value should be(None)
  }

  private def filterWithMocks: RequireAccountEmail = {
    new RequireAccountEmail(mockAccountStore)
  }
  
  def mockAccountStore = {
    val accountStore = mock[AccountStore]
    accountStore.findByEmail(emailWithAccount) returns Some(validAccount)
    accountStore.findByEmail(emailWithoutAccount) returns None

    accountStore
  }
}