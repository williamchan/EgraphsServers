package utils

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import play.api.mvc.Controller
import org.scalatest.FlatSpec
import play.api.mvc.Call
import controllers.routes.WebsiteControllers.getLoginAdmin
import services.db.{DBSession, TransactionSerializable}
import models._

trait AdminProtectedResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  protected def db : DBSession
  
  routeName(routeUnderTest) + ", as an admin authenticated resource, " should "fail due to lack of an admin id in the session" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(routeUnderTest).withAuthToken)
    status(result) should be (SEE_OTHER)
    headers(result)("Location") should be (getLoginAdmin.url)
  }
  
  it should "not redirect to the login page session" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(routeUnderTest).withAdmin(admin.id).withAuthToken)
    redirectLocation(result) should not be (Some(getLoginAdmin.url))
  }
  
  def admin : Administrator = {
    db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}
  }
}

