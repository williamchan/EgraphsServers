package services.http

import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import play.mvc.Http.Request
import models.{Account, AccountStore}
import play.test.{FunctionalTest, UnitFlatSpec}
import models.AccountAuthenticationError.{AccountNotFoundError, AccountPasswordNotSetError, AccountCredentialsError}
import play.mvc.results.Forbidden

class AccountRequestFiltersTests extends UnitFlatSpec with Mockito with ShouldMatchers {


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
    new AccountRequestFilters(accountStore).requireAuthenticatedAccount(blockToExecute)

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
      val result = new AccountRequestFilters(accountStore)
        .requireAuthenticatedAccount(blockThatShouldNotExecute)

      // Check expectations
      result.isInstanceOf[Forbidden] should be (true)
      there was no(blockThatShouldNotExecute).apply(any)

    }
  }

}