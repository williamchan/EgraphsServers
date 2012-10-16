package egraphs.playutils

import play.api.mvc.Results._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.FlatSpec
import RichResult._
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Session
import org.specs2.mock.Mockito
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.mvc.WrappedRequest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RichResultTests extends FlatSpec with ShouldMatchers {
  "addingToSession" should "add to the result if the result already had a SET_COOKIE value" in {
    runningCookieCompatibleApp {
      val result = Ok.withSession("resultKey1" -> "resultValue1")
      
      implicit val request = new WrappedRequest(FakeRequest()) {
        override lazy val session = Session(Map("requestKey" -> "requestValue"))
      }
      
      val maybeNewSession = result.addingToSession("resultKey2" -> "resultValue2").session
      val maybeSessionSet = maybeNewSession.map(sess => sess.data.toSet)
      val expectedSessionSet = Set("resultKey1" -> "resultValue1", "resultKey2" -> "resultValue2")
      
      maybeSessionSet should be (Some(expectedSessionSet))
    }
  }
  
  it should "add to the request session if no session had yet been set onto the result" in {
    runningCookieCompatibleApp {
      val result = Ok
      
      implicit val request = new WrappedRequest(FakeRequest()) {
        override lazy val session = Session(Map("key1" -> "value1"))
      }
      
      val maybeNewSession = result.addingToSession("key2" -> "value2").session
      val maybeSessionSet = maybeNewSession.map(sess => sess.data.toSet) 
      maybeSessionSet should be (Some(Set(("key1" -> "value1"), ("key2" -> "value2"))))
    }
  }

  "withSession" should "overwrite any previous SET_COOKIE values in the result" in {
    runningCookieCompatibleApp {
      // First set the session using the non-implicit version
      val result: play.api.mvc.Result = Ok.withSession("key" -> "value")

      // Now set the session using our implicit version
      val testResult = result.withSession(Session(Map("this was set in" -> "our version")))

      testResult.session.map(sess => sess.data.toList) should be (Some(List("this was set in" -> "our version")))
    }
  }

  "session" should "return Some(value) if it existed in a SET_COOKIE header" in {
    runningCookieCompatibleApp {
      val maybeSession = Ok.withSession("key" -> "value").session
  
      maybeSession.map(session => session.data.toList) should be (Some(List("key" -> "value")))
    }
  }

  it should "return None if no SET_COOKIE header had yet been set" in {
    Ok.session should be (None)
  }
  
  //
  // Private members
  //
  /** 
   * Need to be running an application to sign cookies because play needs an application.secret from
   * the config in order to sign cookies.
   **/
  def runningCookieCompatibleApp(operation: => Any) = {
    running(FakeApplication(additionalConfiguration=Map("application.secret" -> "herp")))(operation)
  }

}