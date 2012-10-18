package services.http
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.EgraphsUnitTest
import play.api.test.FakeRequest

@RunWith(classOf[JUnitRunner])
class BasicAuthTests extends EgraphsUnitTest {
  "Credentials" should "give valid credentials if it was given authentication data that is authentic" in {
    val expectedCredentials = BasicAuth.Credentials("thisIsAUsername", "thisIsAPassword")
    val request = FakeRequest().withHeaders(("Authorization", "Basic " + expectedCredentials.encoded))

    val credentials = BasicAuth.Credentials(request)
    credentials should be (Some(expectedCredentials))
  }

  it should "give different credentials if it was had different user and password" in {
    val someCredentials = BasicAuth.Credentials("thisIsAUsername", "thisIsAPassword")

    val differentCredentials = BasicAuth.Credentials("other", "password")
    val request = FakeRequest().withHeaders(("Authorization", "Basic " + differentCredentials.encoded))

    val credentials = BasicAuth.Credentials(request)
    credentials should not be (someCredentials)
  }
  
  it should "give no credentials if it was had different user and password" in {
    val poorlyEncodedCredentials = "aPoorlyEncodedCredential"
    val request = FakeRequest().withHeaders(("Authorization", "Basic " + poorlyEncodedCredentials))

    val credentials = BasicAuth.Credentials(request)
    credentials should be (None)
  }
}