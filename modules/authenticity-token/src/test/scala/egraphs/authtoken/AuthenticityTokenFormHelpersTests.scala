package egraphs.authtoken

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.templates.Html
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AuthenticityTokenFormHelpersTests extends FlatSpec with ShouldMatchers {
  "AuthenticityToken.hiddenInput" should "inject a hidden input" in {
    implicit val token = new AuthenticityToken("test-token")

    AuthenticityToken.hiddenInput.toString should be(
      """<input type="hidden" name="authenticityToken" value="test-token" />""")
  }

  "AuthenticityToken.safeForm" should "inject a form with a hidden input" in {
    implicit val token = new AuthenticityToken("token-secret")

    val formHtml = AuthenticityToken.safeForm('prop1 -> "value1", 'prop2 -> "value2")(Html("content"))

    formHtml.toString should be(
      """<form prop1="value1" prop2="value2"><input type="hidden" name="authenticityToken" value="token-secret" />content</form>""")
  }

  "AuthenticityToken.asJsonObject" should "convert the object to html in the correct format" in {
    implicit val token = new AuthenticityToken("token-secret")

    val jsonObject = AuthenticityToken.asJsonObject

    jsonObject.toString should be(
      """{"authenticityToken":"token-secret"}""")
  }
}