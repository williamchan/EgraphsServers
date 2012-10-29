package services.http.filters

import models.CelebrityServices
import models.Celebrity
import models.CelebrityStore
import models.enums.PublishedStatus
import play.mvc.Http.Request
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import java.util.Properties
import services.Utils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.doReturn
import services.db.Saves
import services.db.SavesWithLongKey
import services.AppConfig
import utils.DBTransactionPerTest
import utils.TestData
import play.api.mvc.Cookie
import play.api.mvc.Result
import models.Account

@RunWith(classOf[JUnitRunner])
class RequireCelebrityIdTests extends EgraphsUnitTest with DBTransactionPerTest {
  // Myyk: I used DB instead of Mocks here since Mockito wasn't having a good time and Erem and I couldn't figure out how to fix it.
  // It seemed to be an issue with the mocking layer itself since RequireAuthenticatedAccountTests was almost identical and working.

  private val badCelebrityId = Long.MaxValue // We probably don't have a celebrity with this id since they are sequential
  private val badAccountId = Long.MaxValue // We probably don't have a account with this id since they are sequential
  private def badAccount = new Account(id = badAccountId) // unknown account to our db

  private val emptyRequest = FakeRequest()
  private def requestWithCelebrity(celebrityId: Long) = FakeRequest().withFormUrlEncodedBody(("celebrityId", celebrityId.toString))
  private def requestWithBadCelebrity = requestWithCelebrity(badCelebrityId) 

  "asEither" should "find the celebrity if it is in the db and it should be on the right" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.asEither(celebrity.id)
    errorOrCelebrity should be (Right(celebrity))
  }

  it should "contain a Forbidden on the left if there is no celebrity ID found" in new EgraphsTestApplication {
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.asEither(badCelebrityId)
    errorOrCelebrity.isLeft should be (true)
    status(errorOrCelebrity.fold(error => error, celeb => Ok)) should be (FORBIDDEN)
  }

  "asEitherInAccount" should "find the celebrity if it is in the db and it should be on the right" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.asEitherInAccount(celebrity.account)
    errorOrCelebrity should be (Right(celebrity))
  }

  it should "contain a Forbidden on the left if there is no celebrity ID found in that account" in new EgraphsTestApplication {
    val account = TestData.newSavedAccount()
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.asEitherInAccount(account)
    errorOrCelebrity.isLeft should be (true)
    status(errorOrCelebrity.fold(error => error, celeb => Ok)) should be (FORBIDDEN)
  }

  it should "contain a Forbidden on the left if there is no account found" in new EgraphsTestApplication {
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.asEitherInAccount(badAccount)
    errorOrCelebrity.isLeft should be (true)
    status(errorOrCelebrity.fold(error => error, celeb => Ok)) should be (FORBIDDEN)
  }

  "apply" should "execute the provided block if a matching celebrity account was found" in new EgraphsTestApplication {
    // Set up the mock operation to be performed on the authenticated account
    val celebrity = TestData.newSavedCelebrity()
    happyCelebrityFoundTest(celebrity)(
      testOperation = (filter, blockToExecute) => filter(celebrity.id)(blockToExecute)
    )
  }

  it should "be fine even if there is a bad celebrity in the request since that isn't being validated here" in new EgraphsTestApplication {
    // Set up the mock operation to be performed on the authenticated account
    val celebrity = TestData.newSavedCelebrity()
    happyCelebrityFoundTest(celebrity, requestWithBadCelebrity)(
      testOperation = (filter, blockToExecute) => filter(celebrity.id)(blockToExecute)
    )
  }

  it should "not execute the provided block if there was no celebrity in request" in new EgraphsTestApplication {
    noCelebrityFoundTest()(
      testOperation = (filter, blockToExecute) => filter(badCelebrityId)(blockToExecute)
    )
  }

  it should "not execute the provided block if there was no matching celebrity account" in new EgraphsTestApplication {
    noCelebrityFoundTest()(
      testOperation = (filter, blockToExecute) => filter(badCelebrityId)(blockToExecute)
    )
  }

  "inRequest" should "execute the provided block if a matching celebrity account was found in the request" in new EgraphsTestApplication {
    // Our request needs a celebrity id so that it should execute the body.
    val celebrity = TestData.newSavedCelebrity()
    val request = requestWithCelebrity(celebrity.id)

    happyCelebrityFoundTest(celebrity, request)(
      testOperation = (filter, blockToExecute) => filter.inRequest()(blockToExecute)
    )
  }

  it should "not execute the provided block if a bad celebrity id was found in the request" in new EgraphsTestApplication {
    // Our request needs a bad celebrity Id
    val request = requestWithBadCelebrity

    noCelebrityFoundTest(request)(
      testOperation = (filter, blockToExecute) => filter.inRequest()(blockToExecute)
    )
  }

  it should "not execute the provided block if a no celebrity id was found in the request" in new EgraphsTestApplication {
    val request = FakeRequest()
    
    noCelebrityFoundTest(request)(
      testOperation = (filter, blockToExecute) => filter.inRequest()(blockToExecute)
    )
  }

  "inAccount" should "execute the provided block if a matching celebrity account was found in the db" in new EgraphsTestApplication {
    // We need to have a saved celebrity account in the db.
    val celebrity = TestData.newSavedCelebrity()

    happyCelebrityFoundTest(celebrity)(
      testOperation = (filter, blockToExecute) => filter.inAccount(celebrity.account)(blockToExecute)
    )
  }

  it should "not execute the block if the account is not a celebrity account" in new EgraphsTestApplication {
    // We need to have a non-celebrity account in the db that we will use.
    val account = TestData.newSavedAccount()

    noCelebrityFoundTest()(
      testOperation = (filter, blockToExecute) => filter.inAccount(account)(blockToExecute)
    )
  }

  it should "not execute the block if the account is not in out db" in new EgraphsTestApplication {
    // We need to have a non-celebrity account in the db that we will use.
    val account = badAccount

    noCelebrityFoundTest()(
      testOperation = (filter, blockToExecute) => filter.inAccount(account)(blockToExecute)
    )
  }

  private def happyCelebrityFoundTest[A <: AnyContent](celebrity: => Celebrity, request: => FakeRequest[A] = emptyRequest)
  (testOperation: (RequireCelebrityId, Celebrity => Action[AnyContent]) => Action[AnyContent]) = {
    test(request)(
      setupOperation = {
        val accountAction = mock[Action[AnyContent]]
        blockToExecute => blockToExecute(any) returns accountAction

        accountAction(request) returns Ok
      },
      testOperation,
      verification = (result, blockToExecute) => {
        there was one(blockToExecute).apply(celebrity)
        status(result) should be (OK)
      }
    )
  }

  private def noCelebrityFoundTest[A <: AnyContent](request: => FakeRequest[A] = emptyRequest)
  (testOperation: (RequireCelebrityId, Celebrity => Action[AnyContent]) => Action[AnyContent]) = {
    test(request)(
      setupOperation = {
        val accountAction = mock[Action[AnyContent]]
        blockToExecute => blockToExecute(any) returns accountAction

        accountAction(request) returns Ok
      },
      testOperation,
      verification = (result, blockToExecute) => {
        there was no(blockToExecute).apply(any)
        status(result) should be (FORBIDDEN)
      }
    )
  }
  
  private def test[A <: AnyContent](request: FakeRequest[A] = emptyRequest)
  (setupOperation: (Celebrity => Action[AnyContent]) => Any,
  testOperation: (RequireCelebrityId, Celebrity => Action[AnyContent]) => Action[AnyContent],
  verification: (Result, Celebrity => Action[AnyContent]) => Any) = {
    // Set up the mock operation to be performed on the authenticated account
    val blockToExecute = mock[Celebrity => Action[AnyContent]]
    setupOperation(blockToExecute)

    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test

    // run the test operation and so we can verify the result
    val result = testOperation(celebrityIdFilter, blockToExecute)(request)

    verification(result, blockToExecute)
  }
  
  private def newRequireCelebrityId: RequireCelebrityId = {
    val celebrityStore = AppConfig.instance[CelebrityStore]
    new RequireCelebrityId(celebrityStore)
  }
}