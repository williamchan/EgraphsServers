package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.enums.PublishedStatus
import models.{CelebrityAccesskey, Product}
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireProductPublishedAccessTests extends EgraphsUnitTest {
  val publishedProduct = Product(services = null).withPublishedStatus(PublishedStatus.Published)
  val unpublishedProduct = Product(services = null).withPublishedStatus(PublishedStatus.Unpublished)

  "filter" should "allow products that are published" in {
    val errorOrProduct = filter.filter((publishedProduct, ""))
    errorOrProduct should be(Right(publishedProduct))
  }

  it should "not allow products that are not published" in {
    val errorOrProduct = filter.filter((unpublishedProduct, ""))
    status(errorOrProduct.toErrorOrOkResult) should be(NOT_FOUND)
  }

  it should "allow unpublished celebrities if the correct accesskey is provided" in {
    val errorOrProduct = filter.filter((unpublishedProduct, CelebrityAccesskey.accesskey(unpublishedProduct.celebrityId)))
    errorOrProduct should be(Right(unpublishedProduct))
  }

  private def filter: RequireProductPublishedAccess = {
    new RequireProductPublishedAccess()
  }
}