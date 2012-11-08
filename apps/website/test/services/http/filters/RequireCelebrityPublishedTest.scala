package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.enums.PublishedStatus
import models.AccountStore
import models.Celebrity
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireCelebrityPublishedTest extends EgraphsUnitTest {
  val publishedCelebrity = Celebrity(services = null).withPublishedStatus(PublishedStatus.Published)
  val unpublishedCelebrity = Celebrity(services = null).withPublishedStatus(PublishedStatus.Unpublished)

  "filter" should "allow celebrities that are published" in {
    val errorOrCelebrity = filter.filter(publishedCelebrity)

    errorOrCelebrity should be(Right(publishedCelebrity))
  }

  it should "not allow celebrities that are not published" in {
    val errorOrCelebrity = filter.filter(unpublishedCelebrity)
    val result = errorOrCelebrity.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filter: RequireCelebrityPublished = {
    new RequireCelebrityPublished()
  }
}