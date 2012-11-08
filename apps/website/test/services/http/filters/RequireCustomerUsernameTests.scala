package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import models.Customer
import models.CustomerStore
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireCustomerUsernameTests extends EgraphsUnitTest {
  val goodUsername = "goodUsername"
  val badUsername = "badUsername"

  val customer = Customer(services = null)

  "filter" should "allow customer ids that are associated with a customer" in {
    val errorOrCustomer = filterWithMocks.filter(goodUsername)

    errorOrCustomer should be(Right(customer))
  }

  it should "not allow customer ids that are not associated with a customer" in {
    val errorOrCustomer = filterWithMocks.filter(badUsername)
    val result = errorOrCustomer.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks: RequireCustomerUsername = {
    new RequireCustomerUsername(mockCustomerStore)
  }

  private def mockCustomerStore = {
    val customerStore = mock[CustomerStore]

    customerStore.findByUsername(goodUsername) returns Some(customer)
    customerStore.findByUsername(badUsername) returns None

    customerStore
  }
}