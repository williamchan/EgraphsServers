package services.http

import services.http.EgraphsSession.Conversions._
import services.AppConfig
import utils.EgraphsUnitTest
import play.api.mvc.Session
import play.test.FakeRequest
import org.apache.commons.lang3.RandomStringUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EgraphsSessionTests extends EgraphsUnitTest {
  import EgraphsSession.Key

  "session.id" should "be implicitly converted to an EgraphsSession and return the id if set" in {
    test(
      {(random, session) => session + (EgraphsSession.SESSION_ID_KEY -> random)},
      {(random, session) => session.id should be (Some(random))}
    )
  }
  
  it should "return None if not set" in {
    Session.emptyCookie.id should be (None)
  }

  "session.adminId" should "be implicitly converted to an EgraphsSession and return the None if set" in {
    test(
      {(random, session) => session.withAdminId(random.toLong)},
      {(random, session) => session.adminId should be (Some(random.toLong))}
    )
  }
  
  it should "return None if not set" in {
    Session.emptyCookie.adminId should be (None)
  }

  "session.customerId" should "be implicitly converted to an EgraphsSession and return the None if set" in {
    test(
      {(random, session) => session.withCustomerId(random.toLong)},
      {(random, session) => session.customerId should be (Some(random.toLong))}
    )
  }
  
  it should "return None if not set" in {
    Session.emptyCookie.customerId should be (None)
  }

  private def test(modifySessionWithRandom: (String, Session) => Session, aShouldBeStatement: (String, Session) => Unit) {
    val randomString = RandomStringUtils.randomNumeric(10)
    val session = modifySessionWithRandom(randomString, Session.emptyCookie)

    aShouldBeStatement(randomString, session)
  }
}
