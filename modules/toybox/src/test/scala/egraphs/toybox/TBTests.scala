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
class TBTests extends FlatSpec with ShouldMatchers {

  /** TBPrivate test cases **/
  "A TBPrivate" should "redirect unauthorized users to login" in (pending)

  it should "let authorized users through" in (pending)

  /** TBPublic test cases **/
  "A TBPublic" should "let users through regardless of authorization cookies" in (pending)

}