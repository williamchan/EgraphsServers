package controllers.api

import utils.{EgraphsUnitTest, TestConstants, TestData}
import play.api.test.Helpers._
import play.api.mvc._
import utils.FunctionalTestUtils._
import play.api.test.FakeRequest
import services.AppConfig
import services.http.BasicAuth
import services.db.{DBSession, TransactionSerializable}
import models.Account
import org.apache.commons.lang3.RandomStringUtils

trait ProtectedCelebrityResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  protected def validRequestBodyAndQueryString: Option[FakeRequest[AnyContentAsEmpty.type]] = None
  private def db = AppConfig.instance[DBSession]
  
  aBasicAuthProtectedCelebApiResource should "forbid requests with the wrong password for a valid celebrity account" in new EgraphsTestApplication {
    val celebrityAccount = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      celebrity.account
    }

    // Assemble the request
    executingRequestWithCredentials(celebrityAccount, "wrong") should be (FORBIDDEN)
  }

  it should "forbid requests with the correct password for non-celebrity account" in new EgraphsTestApplication {
    val password = RandomStringUtils.random(12)
    val dbSession = AppConfig.instance[DBSession]

    val account = dbSession.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()      

      customer.account.withPassword(password).right.get.save()
    }

    executingRequestWithCredentials(account, password) should be (FORBIDDEN)
  }

  private def executingRequestWithCredentials(account: Account, password: String): Int = {
    val auth = BasicAuth.Credentials(account.email, password)

    val requestBodyAndQuery = validRequestBodyAndQueryString.getOrElse(FakeRequest(routeUnderTest.method, routeUnderTest.url))
    val request = requestBodyAndQuery.withCredentials(account, password)

    // Execute the request
    status(route(request).get)
  }

  private def aBasicAuthProtectedCelebApiResource = routeName(routeUnderTest) + ", as a basic-auth protected celebrity API resource,"
}
