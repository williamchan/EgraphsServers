package services.http.filters

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import models.Account
import models.Celebrity
import models.CelebrityStore
import play.api.mvc.Results.Ok
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.test.Helpers.FORBIDDEN
import play.api.test.Helpers.OK
import play.api.test.Helpers.status
import play.api.test.FakeRequest
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import services.AppConfig
import utils.DBTransactionPerTest
import utils.EgraphsUnitTest
import utils.TestData

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

  "filter" should "find the celebrity if it is in the db and it should be on the right" in {
    val celebrity = TestData.newSavedCelebrity()
    val errorOrAccount = newRequireCelebrityId.filter(celebrity.id)

    errorOrAccount should be(Right(celebrity))
  }

  it should "contain a Forbidden on the left if there is no celebrity ID found" in {
    val errorOrAccount = newRequireCelebrityId.filter(badCelebrityId)
    val result = errorOrAccount.toErrorOrOkResult

    status(result) should be(FORBIDDEN)
  }

  "form" should "require an celebrityId" in {
    val celebrity = TestData.newSavedCelebrity()
    val boundForm = newRequireCelebrityId.form.bind(Map("celebrityId" -> celebrity.id.toString))

    boundForm.value should be(Some(celebrity.id))
  }

  it should "fail if it does not have an celebrityId" in {
    val boundForm = newRequireCelebrityId.form.bind(Map.empty[String, String])

    boundForm.value should be(None)
  }

  it should "fail if the celebrityId is not a positive number" in {
    val negativeAdminId = newRequireCelebrityId.form.bind(Map("celebrityId" -> -999L.toString))
    val zeroAdminId = newRequireCelebrityId.form.bind(Map("celebrityId" -> 0.toString))

    negativeAdminId.value should be(None)
    zeroAdminId.value should be(None)
  }

  "filterInAccount" should "find the celebrity if it is in the db and it should be on the right" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.filterInAccount(celebrity.account)
    errorOrCelebrity should be (Right(celebrity))
  }

  it should "contain a Forbidden on the left if there is no celebrity ID found in that account" in new EgraphsTestApplication {
    val account = TestData.newSavedAccount()
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.filterInAccount(account)
    errorOrCelebrity.isLeft should be (true)
    status(errorOrCelebrity.fold(error => error, celeb => Ok)) should be (FORBIDDEN)
  }

  it should "contain a Forbidden on the left if there is no account found" in new EgraphsTestApplication {
    val celebrityIdFilter = newRequireCelebrityId // this filter is what we are trying to test
    
    val errorOrCelebrity = celebrityIdFilter.filterInAccount(badAccount)
    errorOrCelebrity.isLeft should be (true)
    status(errorOrCelebrity.fold(error => error, celeb => Ok)) should be (FORBIDDEN)
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