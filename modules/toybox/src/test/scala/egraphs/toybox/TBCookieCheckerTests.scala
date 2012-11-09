package egraphs.toybox.default

import play.api.mvc.Results
import play.api.http.Status
import play.api.test.FakeApplication
import play.api.test.FakeRequest

import play.api.test.Helpers.status

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class TBCookieCheckerTests extends FlatSpec with ShouldMatchers {
  val mock = TBMockObjects
  val checker = mock.checker

  /** TBPrivate test cases **/
  "A TBCookieChecker" should "throw an exception when given a null or empty cookie name" in {
    try {
      val nullNamedChecker = new TBCookieChecker(null)
      fail()
    } catch {
      case _: IllegalArgumentException =>
      case _ => fail()
    }

    try {
      val emptyNamedChecker = new TBCookieChecker("")
      fail()
    } catch {
      case _: IllegalArgumentException =>
      case _ => fail()
    }
  }

  it should "generate non-null cookies, by default, regardless of request" in {
    assert(checker.generate(FakeRequest()) != None)
    assert(checker.generate(null) != None)
  }
  
  it should "not validate requests that don't contain the authentication cookie" in {
    assert(checker.validate(mock.unauthRequest) == false)
    assert(checker.validate(null) == false)
  }

  it should "validate requests that have the authentication cookie" in {
    assert(checker.validate(mock.authRequest))
  }

}