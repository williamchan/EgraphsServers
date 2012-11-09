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
class TBAuthenticatorTests extends FlatSpec with ShouldMatchers {
  
  // alternatively, could give feedback to caller, presumably a ToyBox, so it
  // can default to public if no password is provided. Seems simple enough to 
  // verify what mode the application is in that it shouldn't get accidentally
  // deployed in public mode when private was the intention.
  "A TBAuthenticator" should "throw an exception if initialized without a password" in {
    try {
      val invalidAuth = new TBAuthenticator(FakeApplication().configuration)
      fail()
    } catch {
      case _: IllegalArgumentException =>
      case _ => fail()
    }
  }

  val mock = TBMockObjects
  val getLoginResult = mock.auth.login(FakeRequest())
  val failLoginResult = mock.auth.authenticate(mock.loginFailureRequest)
  val succLoginResult = mock.auth.authenticate(mock.loginSuccessRequest)

  // this is a pretty trivial test, just using it mainly to get started with scalatest
  it should "return OK for requesting login page" in {
    // TODO: see if it can be verified that the correct route was returned?
    status(getLoginResult) should equal (Status.OK)
  }

  it should "redirect user back to login when authentication fails" in {
    status(failLoginResult) should equal (Status.SEE_OTHER)

    // TODO: check that redirect returns to login page
  }

  it should "redirect user to original page when authentication succeeds" in {
    status(succLoginResult) should equal (Status.SEE_OTHER)

    // TODO: check that redirect goes to "original" requested page
  }

  it should "handle form submission errors gracefully" in (pending)



}