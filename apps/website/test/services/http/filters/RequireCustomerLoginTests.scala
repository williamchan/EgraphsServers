package services.http.filters

import org.junit.runner.RunWith
import models.Account
import models.AccountStore
import play.api.data.Form
import play.api.mvc.Results.Ok
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.status
import utils.EgraphsUnitTest
import org.scalatest.junit.JUnitRunner
import services.http.filters.FilterTestUtil._
import models.CustomerStore
import models.Customer
import models.CustomerStore
import services.AppConfig
import utils.TestData
import utils.DBTransactionPerTest
import services.http.EgraphsSession

@RunWith(classOf[JUnitRunner])
class RequireCustomerLoginTests extends EgraphsUnitTest with DBTransactionPerTest {

  val badCustomerId = Long.MaxValue

  "filter" should "allow customerIds that have a customer and account associated with them" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    val errorOrCustomerAccount = filter.filter(customer.id)

    errorOrCustomerAccount should be(Right(customer, customer.account))
  }

  it should "not allow customerIds that do not have an customer and account associated with them" in {
    val errorOrCustomerAccount = filter.filter(badCustomerId)

    val result = errorOrCustomerAccount.toErrorOrOkResult

    status(result) should be(SEE_OTHER)
  }

  "form" should "require an customerIds" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    val errorOrCustomerAccount = filter.form.bind(Map(EgraphsSession.Key.CustomerId.name -> customer.id.toString))

    errorOrCustomerAccount.value should be(Some(customer.id))
  }

  it should "fail if it does not have an customerIds" in {
    val errorOrCustomerAccount = filter.form.bind(Map.empty[String, String])

    errorOrCustomerAccount.value should be(None)
  }

  it should "fail if the customerId is not a positive number" in {
    val negativeAdminId = filter.form.bind(Map(EgraphsSession.Key.CustomerId.name -> -999L.toString))
    val zeroAdminId = filter.form.bind(Map(EgraphsSession.Key.CustomerId.name -> 0.toString))

    negativeAdminId.value should be(None)
    zeroAdminId.value should be(None)
  }

  private def filter: RequireCustomerLogin = {
    new RequireCustomerLogin(AppConfig.instance[CustomerStore])
  }
}