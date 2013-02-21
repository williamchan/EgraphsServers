package controllers.website

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult
import models.AccountStore
import services.http.forms._
import services.http.forms.Form.Conversions._
import services.http.forms.CustomerLoginForm.Fields
import services.http.EgraphsSession.Conversions._
import services.http.POSTControllerMethod
import services.db._
import services.AppConfig
import services.http.forms.CustomerLoginFormFactory
import utils.FunctionalTestUtils._
import utils._
import controllers.routes.WebsiteControllers.{getLogin, postLogin}


@RunWith(classOf[JUnitRunner])
class LoginTests extends EgraphsUnitTest with ClearsCacheBefore with CsrfProtectedResourceTests {
  import Form.Conversions._
  private def db = AppConfig.instance[DBSession]
  
  override protected def routeUnderTest = postLogin()

  routeName(routeUnderTest) should "redirect to login with form information when passwords don't match" in new EgraphsTestApplication {
    val request = FakeRequest().withFormUrlEncodedBody(
      Fields.Email -> "idontexist@egraphs.com", Fields.Password -> TestData.defaultPassword
    ).withAuthToken
    
    val result = controllers.WebsiteControllers.postLogin().apply(request)

    status(result) should be (SEE_OTHER)
    redirectLocation(result) should be (Some(getLogin().url))

    db.connected(TransactionSerializable) {
      val customerFormOption = AppConfig.instance[CustomerLoginFormFactory].read(result.flash.get.asFormReadable)
      customerFormOption match {
        case Some(form) =>
          form.email.value should be (Some("idontexist@egraphs.com"))
          form.password.value should be (Some(TestData.defaultPassword))

        case None =>
          fail("There should have been serialized form in the flash")
      }
    }
  }

  it should "add customer ID to session when passwords do match" in new EgraphsTestApplication {
    // Set up
    val password = "this is teh password"
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withPassword(password).right.get.save()
    }
    

    val result = controllers.WebsiteControllers.postLogin().apply(
      FakeRequest().withFormUrlEncodedBody(
        Fields.Email -> account.email, Fields.Password -> password
      ).withAuthToken
    )
    val maybeResultCustomerId = result.session.flatMap(session => session.customerId)

    // Check expectations
    status(result) should be (SEE_OTHER)
    redirectLocation(result) should not be (Some(getLogin().url))
    maybeResultCustomerId should be (account.customerId)
  }

  private class MockLoginController extends Controller with PostLoginEndpoint {
    override val postController = Stubs.postControllerMethod
    override val accountStore = AppConfig.instance[AccountStore]
    override val customerLoginForms = AppConfig.instance[CustomerLoginFormFactory]
  }


}