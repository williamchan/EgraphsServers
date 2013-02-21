package controllers.api

import org.apache.commons.lang3.RandomStringUtils
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import utils.FunctionalTestUtils._
import utils.{EgraphsUnitTest, TestConstants, TestData}
import services.AppConfig
import services.http.BasicAuth
import services.db.{DBSession, TransactionSerializable}
import models.Account

trait ProtectedCelebrityResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  // if this is defined you must define routeRequest to route that kind of request
  protected def validRequestBodyAndQueryString: Option[FakeRequest[_]] = None
  protected def routeRequest(request: FakeRequest[_]): Option[Result] = throw new UnsupportedOperationException("routeRequest must be defined if validRequestBodyAndQueryString is defined.")

  private def db = AppConfig.instance[DBSession]
  protected def newRouteUnderTestFakeRequest: FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(routeUnderTest.method, routeUnderTest.url)
  }

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
    validRequestBodyAndQueryString match {
      case Some(req) => status(routeRequest(req.withCredentials(account, password)).get)
      case None => status(route(newRouteUnderTestFakeRequest.withCredentials(account, password)).get)
    }
  }

  private def aBasicAuthProtectedCelebApiResource = routeName(routeUnderTest) + ", as a basic-auth protected celebrity API resource,"
}
