package utils

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils._
import controllers.routes.WebsiteControllers.getLoginAdmin
import services.db.{DBSession, TransactionSerializable}
import models._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, Call}
import play.api.mvc.Action
import services.mvc.MultipartFormTestHelper

abstract trait AdminProtectedResourceTestBase { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  protected def db : DBSession

  def admin : Administrator = {
    db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}
  }
}

trait AdminProtectedResourceTests extends AdminProtectedResourceTestBase { this: EgraphsUnitTest =>
  routeName(routeUnderTest) + ", as an admin authenticated resource, " should "fail due to lack of an admin id in the session" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest(routeUnderTest.method, routeUnderTest.url).withAuthToken)
    status(result) should be (SEE_OTHER)
    headers(result)("Location") should be (getLoginAdmin.url)
  }
  
  it should "not redirect to the login page session" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest(routeUnderTest.method, routeUnderTest.url).withAdmin(admin.id).withAuthToken)
    redirectLocation(result) should not be (Some(getLoginAdmin.url))
  }
}

/**
 * This trait works similarly to AdminProtectedResourceTests, except is designed to test multipart form submissions
 */
trait AdminProtectedMultipartFormResourceTests extends AdminProtectedResourceTestBase with MultipartFormTestHelper { this: EgraphsUnitTest =>

  it should "fail due to lack of an admin id in the session" in new EgraphsTestApplication {
    val result = controllerMethod(request.withAuthToken)

    status(result) should be (SEE_OTHER)
    headers(result)("Location") should be (getLoginAdmin.url)
  }

  it should "not redirect to the login page session" in new EgraphsTestApplication {
    val result = controllerMethod(request.withAdmin(admin.id).withAuthToken)
    redirectLocation(result) should not be (Some(getLoginAdmin.url))
  }

}

