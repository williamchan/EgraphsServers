package services.http.filters

import models.{ Account, AccountStore }
import models.AccountAuthenticationError.{ AccountNotFoundError, AccountPasswordNotSetError, AccountCredentialsError }
import utils.{ ClearsCacheBefore, EgraphsUnitTest }
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
import services.AppConfig
import utils.TestData
import utils.DBTransactionPerTest
import models.enums.PublishedStatus
import services.http.EgraphsSession
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.mvc.SimpleResult

@RunWith(classOf[JUnitRunner])
class RequireCelebrityAndProductUrlSlugsTest extends EgraphsUnitTest with DBTransactionPerTest {

  class ThisTestPassedException extends Exception

  "apply" should "execute the actionFactory provided when it passes all checks" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity))

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest()

    intercept[ThisTestPassedException] {
      filter(celebrity.urlSlug, product.urlSlug) {
        (celeb, prod) => throw new ThisTestPassedException
      }(request)
    }
  }

  it should "execute the actionFactory provided when it passes all checks and is admin" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity))
    val admin = TestData.newSavedAdministrator()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest().withSession((EgraphsSession.Key.AdminId.name, admin.id.toString))

    intercept[ThisTestPassedException] {
      filter(celebrity.urlSlug, product.urlSlug) {
        (celeb, prod) => throw new ThisTestPassedException
      }(request)
    }
  }

  it should "execute the actionFactory provided when it passes urlSlug checks but is not published and is admin" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Unpublished).save()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity)).withPublishedStatus(PublishedStatus.Unpublished).save()
    val admin = TestData.newSavedAdministrator()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest().withSession((EgraphsSession.Key.AdminId.name, admin.id.toString))

    intercept[ThisTestPassedException] {
      filter(celebrity.urlSlug, product.urlSlug) {
        (celeb, prod) => throw new ThisTestPassedException
      }(request)
    }
  }

  it should "not execute the actionFactory provided when it does pass urlSlug checks but is not published and is not admin" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Unpublished).save()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity)).withPublishedStatus(PublishedStatus.Unpublished).save()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest()

    val result = filter(celebrity.urlSlug, product.urlSlug) {
      (celeb, prod) => fail()
    }(request)

    status(result) should not be (OK)
  }

  it should "not execute the actionFactory provided when it does not pass urlSlug checks" in new EgraphsTestApplication {
    // Set up DB
    val celebrityUrl = RandomStringUtils.random(20)
    val productUrl = RandomStringUtils.random(20)
    val admin = TestData.newSavedAdministrator()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    // should no execute actionFactory even though request has valid admin
    implicit val request = FakeRequest().withSession((EgraphsSession.Key.AdminId.name, admin.id.toString))

    val result = filter(celebrityUrl, productUrl) {
      (celeb, prod) => fail()
    }(request)

    status(result) should not be (OK)
  }

  "filter" should "provided the celebrity and product when it passes all checks" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity))

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest()

    val errorOrCelebrityAndProduct = filter.filter(celebrity.urlSlug, product.urlSlug)(request)

    errorOrCelebrityAndProduct should be(Right(celebrity, product))
  }

  it should "provided the celebrity and product when it passes all checks and is admin" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity))
    val admin = TestData.newSavedAdministrator()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest().withSession((EgraphsSession.Key.AdminId.name, admin.id.toString))

    val errorOrCelebrityAndProduct = filter.filter(celebrity.urlSlug, product.urlSlug)(request)

    errorOrCelebrityAndProduct should be(Right(celebrity, product))
  }

  it should "provided the celebrity and product when it passes urlSlug checks but is not published and is admin" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Unpublished).save()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity)).withPublishedStatus(PublishedStatus.Unpublished).save()
    val admin = TestData.newSavedAdministrator()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest().withSession((EgraphsSession.Key.AdminId.name, admin.id.toString))

    val errorOrCelebrityAndProduct = filter.filter(celebrity.urlSlug, product.urlSlug)(request)

    errorOrCelebrityAndProduct should be(Right(celebrity, product))
  }

  it should "provide an error result when it does pass urlSlug checks but is not published and is not admin" in new EgraphsTestApplication {
    // Set up DB
    val celebrity = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Unpublished).save()
    val product = TestData.newSavedProductWithoutInventoryBatch(Some(celebrity)).withPublishedStatus(PublishedStatus.Unpublished).save()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    implicit val request = FakeRequest()

    val errorOrCelebrityAndProduct = filter.filter(celebrity.urlSlug, product.urlSlug)(request)
    val result = errorOrCelebrityAndProduct.toErrorOrOkResult

    status(result) should not be(Ok)
  }

  it should "provide an error result when it does not pass urlSlug checks" in new EgraphsTestApplication {
    // Set up DB
    val celebrityUrl = RandomStringUtils.random(20)
    val productUrl = RandomStringUtils.random(20)
    val admin = TestData.newSavedAdministrator()

    val filter = AppConfig.instance[RequireCelebrityAndProductUrlSlugs] // this is what we are trying to test

    // should pass checks even though request has valid admin
    implicit val request = FakeRequest().withSession((EgraphsSession.Key.AdminId.name, admin.id.toString))

    val errorOrCelebrityAndProduct = filter.filter(celebrityUrl, productUrl)(request)
    val result = errorOrCelebrityAndProduct.toErrorOrOkResult

    status(result) should not be (OK)
  }
}