package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.enums.PublishedStatus
import models.Product
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireProductPublishedTests extends EgraphsUnitTest {
  val publishedProduct = Product(services = null).withPublishedStatus(PublishedStatus.Published)
  val unpublishedProduct = Product(services = null).withPublishedStatus(PublishedStatus.Unpublished)

  "filter" should "allow products that are published" in {
    val errorOrCelebrity = filter.filter(publishedProduct)

    errorOrCelebrity should be(Right(publishedProduct))
  }

  it should "not allow products that are not published" in {
    val errorOrCelebrity = filter.filter(unpublishedProduct)
    val result = errorOrCelebrity.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filter: RequireProductPublished = {
    new RequireProductPublished()
  }
}