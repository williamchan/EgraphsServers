package egraphs.toybox.default

import play.api.mvc.Results
import play.api.http.Status
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class TBAuthenticatorTests extends FlatSpec with ShouldMatchers {

    val failApp = new FakeApplication(additionalConfiguration = 
        Map(
            "toybox-username" -> "username",
            "toybox-password" -> "password"
        )
    )

    val successApp = new FakeApplication(additionalConfiguration = 
        Map(
            "toybox-username" -> "username",
            "toybox-password" -> "password"
        )
    )


    "A TBAuthenticator" should "return a login page for unauthenticated users" in {
        
        val auth = new TBAuthenticator(successApp.configuration)
        val result = auth.login(FakeRequest())
        Helpers.status(result) should equal (Status.OK)
    }

    it should "authenticate a user's form submission against the config file" in (pending)

    it should "redirect user back to login when authentication fails" in (pending)

    it should "redirect user to original page when authentication succeeds" in (pending)

}