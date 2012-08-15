package services.http

import models.{Account, AccountStore}
import play.test.FunctionalTest
import models.AccountAuthenticationError.{AccountNotFoundError, AccountPasswordNotSetError, AccountCredentialsError}
import play.mvc.results.Forbidden
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}

class AccountRequestFiltersTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {

  "requiresAuthenticatedAccount" should "execute the provided block if a matching account was found" in {
    // Set up
    implicit val request = FunctionalTest.newRequest()
    request.user = "user"
    request.password = "password"

    val account = mock[Account]

    val accountStore = mock[AccountStore]
    accountStore.authenticate("user", "password") returns Right(account)

    val blockToExecute = mock[Function1[Account,  Any]]

    // Execute test
    new AccountRequestFilters(accountStore, null).requireAuthenticatedAccount(blockToExecute)

    // Check expectations
    there was one(blockToExecute).apply(account)
  }

  it should "return 403 Forbidden when the account failed to authenticate" in {
    implicit val request = FunctionalTest.newRequest()
    request.user = "user"
    request.password = "password"

    val allErrors = List(
      new AccountCredentialsError, new AccountPasswordNotSetError, new AccountNotFoundError
    )

    for (authError <- allErrors) {
      val accountStore = mock[AccountStore]
      accountStore.authenticate("user", "password") returns Left(authError)
      
      val blockThatShouldNotExecute= mock[Function1[Account, Any]]
      
      // Execute
      val result = new AccountRequestFilters(accountStore, null)
        .requireAuthenticatedAccount(blockThatShouldNotExecute)

      // Check expectations
      result.isInstanceOf[Forbidden] should be (true)
      there was no(blockThatShouldNotExecute).apply(any)

    }
  }

}