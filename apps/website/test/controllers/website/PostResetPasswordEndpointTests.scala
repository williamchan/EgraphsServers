package controllers.website

import services.Utils
import services.db.TransactionSerializable
import utils.TestData
import controllers.WebsiteControllers
import utils.EgraphsUnitTest
import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult

import play.api.mvc.Result
import controllers.routes.WebsiteControllers.{postResetPassword, getResetPassword}
import services.AppConfig
import services.http.forms.AccountPasswordResetForm.Fields
import utils.FunctionalTestUtils._
import services.db.DBSession
import play.api.mvc.Controller
import com.google.inject.Inject
import services.http.filters.HttpFilters
import models.AccountStore
import services.http.forms.AccountPasswordResetFormFactory
import utils.Stubs
import services.mvc.ImplicitHeaderAndFooterData
import models.Account
import utils.CsrfProtectedResourceTests

class PostResetPasswordEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests {
  import controllers.WebsiteControllers
  import PostResetPasswordEndpoint._
  
  override protected def routeUnderTest = postResetPassword
  
  private def db = AppConfig.instance[DBSession]
  
  routeName(routeUnderTest) should "set a new password when the new one and its confirmation match and it meets the strength requirement" in new EgraphsTestApplication {
    val result = performPostResetPassword(accountWithResetPasswordKey)("password", "password")

    // We expect a redirect to a success message here.
    redirectLocation(result) should be (Some(successTarget.url))

    formErrors(result) should be (None)
  }

  it should "redirect requests whose password and password confirmation didn't match back to getResetPassword" in new EgraphsTestApplication {
    val account = accountWithResetPasswordKey
    val result = performPostResetPassword(account)("password1", "password2")

    status(result) should be (SEE_OTHER)
    redirectLocation(result) should be (Some(getResetPassword(account.email, account.resetPasswordKey.get).url)) 
    
    formErrors(result) should not be (None)
  }

  it should "redirect requests whose minimum password strength is not met back to getResetPassword" in new EgraphsTestApplication {
    val result = performPostResetPassword(accountWithResetPasswordKey)("p", "p")
    
    status(result) should be (SEE_OTHER)
    formErrors(result) should not be (None)
  }
  
  it should "redirect requests with the wrong secret key back to getResetPassword" in new EgraphsTestApplication {
    val account = accountWithResetPasswordKey
    val wrongKey = "wrong"
    val result = performPostResetPassword(account)("password", "password", secretKey=wrongKey)

    status(result) should be (SEE_OTHER)
    redirectLocation(result) should be (Some(getResetPassword(account.email, wrongKey).url)) 
    
    formErrors(result) should not be (None)
  } 
  
  
  //
  // Private members
  //
  private def formErrors(result: Result): Option[String] = {
    result.flash.flatMap(_.get("AccountPasswordResetForm.errors"))
  }

  private def accountWithResetPasswordKey: Account = {
    db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }
  }
  private def performPostResetPassword(account: Account)(
    newPass: String,
    passConfirm: String,
    secretKey: String = account.resetPasswordKey.get,
    email: String = account.email
  ): Result = 
  {
    val request = FakeRequest().withFormUrlEncodedBody(
      Fields.SecretKey.name -> secretKey,
      Fields.Email.name -> email,
      Fields.NewPassword.name -> newPass, 
      Fields.PasswordConfirm.name -> passConfirm
    )
    
    controllers.WebsiteControllers.postResetPassword().apply(request.withAuthToken)
  }
}
