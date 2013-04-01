package controllers.website

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult
import services.http.EgraphsSession.Conversions._
import services.db._
import services.AppConfig
import utils.FunctionalTestUtils._
import utils._
import controllers.routes.WebsiteControllers.{getLogin, postLogin}


@RunWith(classOf[JUnitRunner])
class LoginTests extends EgraphsUnitTest with ClearsCacheBefore with CsrfProtectedResourceTests {
  import services.login.PostLoginHelper.keys
  
  private def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = postLogin()

  routeName(routeUnderTest) should "redirect to login with form information when account doesn't exist" in new EgraphsTestApplication {
    val badEmail = "idontexist@egraphs.com"
    val Some(result) = route(
      FakeRequest().toCall(routeUnderTest).withFormUrlEncodedBody(
        keys.email -> badEmail,
        keys.password -> TestData.defaultPassword
      ).withAuthToken
    )

    status(result) should be (SEE_OTHER)
    redirectLocation(result) should be (Some(getLogin().url))

    // Email and password should be stored in the flash
    result.flash.get.get(keys.email) should be (Some(badEmail))
    result.flash.get.get(keys.password) should be (Some(TestData.defaultPassword))
  }

  routeName(routeUnderTest) should "redirect to login with form information when given invalid password" in new EgraphsTestApplication {
    val password = "this is the password"
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withPassword(password).right.get.save()
    }

    val Some(result) = route(
      FakeRequest().toCall(routeUnderTest).withFormUrlEncodedBody(
        keys.email -> account.email,
        keys.password -> "wrong password").withAuthToken
    )

    status(result) should be (SEE_OTHER)
    redirectLocation(result) should be (Some(getLogin().url))
  }

  it should "add customer ID to session when passwords do match" in new EgraphsTestApplication {
    val password = "real password"
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withPassword(password).right.get.save()
    }

    val Some(result) = route(
      FakeRequest().toCall(routeUnderTest).withFormUrlEncodedBody(
        keys.email -> account.email,
        keys.password -> "real password").withAuthToken
    )

    val maybeResultCustomerId = result.session.flatMap(session => session.customerId)

    status(result) should be (SEE_OTHER)
    redirectLocation(result) should not be (Some(getLogin().url))
    maybeResultCustomerId should be (account.customerId)
  }
}