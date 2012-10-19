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
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import org.mockito.Mockito.doReturn

@RunWith(classOf[JUnitRunner])
class RequireAuthenticatedAccountTests extends EgraphsUnitTest {

  "requiresAuthenticatedAccount" should "execute the provided block if a matching account was found in basic authentication" in {
    // Set up "DB"
    val userEmail = "myyk@dadshouse.com"
    val password = "catdoghouse"
    val accountStore = mock[AccountStore]
    val account = Account(email = userEmail, services = mock[AccountServices]).withPassword(password).fold(ex => throw new Exception("Bad test setup."), a => a)
    accountStore.authenticate(userEmail, password) returns Right(account)

    // Set up the request to look right with proper credentials
    val credentials = BasicAuth.Credentials(userEmail, password)
    val request = FakeRequest().withHeaders(credentials.toHeader)

    // Set up the mock operation to be performed on the authenicated account
    val accountAction = mock[Action[AnyContent]]
    val blockToExecute = mock[Account => Action[AnyContent]]
    blockToExecute(account) returns accountAction
    accountAction(request) returns Ok

    val authenticatedAccountFilter = new RequireAuthenticatedAccount(accountStore) // this is what we are trying to test
    
    // Execute test
    val result = authenticatedAccountFilter()(blockToExecute)(request)
    
    // Check expectations
    there was one(blockToExecute).apply(account)
    status(result) should be (OK)
  }

  it should "return 403 Forbidden when the basic authentication fails get credentials from the request" in {
    // Set up
    val accountStore = mock[AccountStore]

    val authenticatedAccountFilter = new RequireAuthenticatedAccount(accountStore) // this is what we are trying to test

    val blockToExecute = mock[Account => Action[AnyContent]]

    val request = FakeRequest()

    // Execute test
    val result = authenticatedAccountFilter()(blockToExecute)(request)
    
    // Check expectations
    status(result) should be (FORBIDDEN)
    there was no(blockToExecute).apply(any)
  }

  it should "return 403 Forbidden when the account failed to authenticate" in {
    // Set up
    val userEmail = "myyk@dadshouse.com"
    val password = "catdoghouse"
    val accountStore = mock[AccountStore]
    accountStore.authenticate(userEmail, password) returns Left(new AccountNotFoundError)

    val authenticatedAccountFilter = new RequireAuthenticatedAccount(accountStore) // this is what we are trying to test

    val blockToExecute = mock[Account => Action[AnyContent]]

    val request = FakeRequest()
    val credentials = BasicAuth.Credentials(userEmail, password)
    request.withHeaders(credentials.toHeader)

    // Execute test
    val result = authenticatedAccountFilter()(blockToExecute)(request)
    
    // Check expectations
    status(result) should be (FORBIDDEN)
    there was no(blockToExecute).apply(any)
    there was one(accountStore).authenticate(userEmail, password)
  }
}