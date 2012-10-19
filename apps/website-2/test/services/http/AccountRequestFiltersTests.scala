package services.http.filters

import models.{Account, AccountStore}
import models.AccountAuthenticationError.{AccountNotFoundError, AccountPasswordNotSetError, AccountCredentialsError}
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import play.api.test.FakeRequest
import org.apache.commons.lang3.RandomStringUtils
import play.api.mvc.Action
import play.api.mvc.AnyContent
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.http.BasicAuth
import models.AccountServices
import play.api.mvc.Results
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class RequireAuthenticatedAccountTests extends EgraphsUnitTest {

  "requiresAuthenticatedAccount" should "execute the provided block if a matching account was found" in {
    // Set up
    val user = "myyk"
    val password = "catdoghouse"
    val accountStore = mock[AccountStore]
    val account = Account(services = mock[AccountServices])
    accountStore.authenticate(user, password) returns Right(account)

    val authenticatedAccountFilter = new RequireAuthenticatedAccount(accountStore) // this is what we are trying to test

    val blockToExecute = mock[Account => Action[AnyContent]]

    val request = FakeRequest()
    val credentials = BasicAuth.Credentials(user, password)
    request.withHeaders(credentials.toHeader)

    // Execute test
    val result = authenticatedAccountFilter()(blockToExecute)(request)
    
    // Check expectations
    status(result) should be (OK)
    there was one(blockToExecute).apply(account)
  }

  it should "return 403 Forbidden when the account failed to authenticate" in {
    // Set up
    val accountStore = mock[AccountStore]
    accountStore.authenticate("unknownUser", "somePassword") returns Left(new AccountNotFoundError)

    val authenticatedAccountFilter = new RequireAuthenticatedAccount(accountStore) // this is what we are trying to test

    val blockToExecute = mock[Account => Action[AnyContent]]

    val request = FakeRequest()
    val credentials = BasicAuth.Credentials("myyk","catdoghouse")
    request.withHeaders(credentials.toHeader)

    // Execute test
    val result = authenticatedAccountFilter()(blockToExecute)(request)
    
    // Check expectations
    status(result) should be (FORBIDDEN)
    there was no(blockToExecute)
  }
}