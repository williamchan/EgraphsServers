package services.http.filters

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import models.Celebrity
import models.CelebrityStore
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireCelebrityUrlSlugTests extends EgraphsUnitTest {
  val goodUrlSlug = "goodSlug"
  val badUrlSlug = "badSlug"
  
  val celebrityWithUrlSlug = Celebrity(publicName = "with", services = null)

  "filter" should "allow celebrities slugs that are associated with a celebrity" in {
    val errorOrCelebrity = filterWithMocks.filter(goodUrlSlug)

    errorOrCelebrity should be(Right(celebrityWithUrlSlug))
  }

  it should "not allow celebrities slugs that are not associated with a celebrity" in {
    val errorOrCelebrity = filterWithMocks.filter(badUrlSlug)
    val result = errorOrCelebrity.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks: RequireCelebrityUrlSlug = {
    new RequireCelebrityUrlSlug(mockCelebrityStore)
  }
  
  private def mockCelebrityStore = {
    val celebrityStore = mock[CelebrityStore]

    celebrityStore.findByUrlSlug(goodUrlSlug) returns Some(celebrityWithUrlSlug)
    celebrityStore.findByUrlSlug(badUrlSlug) returns None

    celebrityStore
  }
}