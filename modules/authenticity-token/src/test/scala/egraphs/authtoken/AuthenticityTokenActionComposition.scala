package egraphs.authtoken

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AuthenticityTokenActionCompositionTests extends FlatSpec with ShouldMatchers {
  "AuthenticityToken.makeAvailable" should "make a newly generated authenticity token available if none was in the session" in (pending)
  it should "make the existing token available if it was found in the session" in (pending)

  "AuthenticityToken.requireInSubmission" should "404 if an authenticity token matching the session token wasn't found" in (pending)
  it should "404 if no token was found in the session " in (pending)
  it should "404 if no token was found in the response" in (pending)
  it should "return a new authenticity token in the session of a 404" in (pending)
  it should "not overwrite existing sessions" in (pending)
}
