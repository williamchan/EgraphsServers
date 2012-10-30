package utils

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import play.api.mvc.Controller
import org.scalatest.FlatSpec
import play.api.mvc.Call
import services.db.{DBSession, TransactionSerializable}
import models._

trait AdminProtectedResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  protected def db : DBSession
  
  routeName(routeUnderTest) + ", as an admin authenticated resource, " should "fail due to lack of an admin id in the session" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(routeUnderTest).withAuthToken)
    
    status(result) should be (SEE_OTHER)

  }
  
  it should "not throw an error if there is an admin id in the session" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(routeUnderTest).withAdmin(admin.id).withAuthToken)
    status(result) should not be (SEE_OTHER)    
  }
  
  def admin : Administrator = {
    db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}
  }
}

