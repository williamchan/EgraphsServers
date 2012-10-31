package services.http.filters

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import models.Celebrity
import models.CelebrityStore
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import utils.EgraphsUnitTest
import models.ProductStore
import models.Product
import models.enums.PublishedStatus
import models.ProductQueryFilters
import services.db.FilterOneTable
import org.squeryl.Query

@RunWith(classOf[JUnitRunner]) //If you ever have to look at this.  Maybe rewrite as an integration test for readability.
class RequireProductUrlSlugTests extends EgraphsUnitTest {
  val goodUrlSlug = "goodSlug"
  val badUrlSlug = "badSlug"

  val productWithUrlSlug = Product(services = null).withPublishedStatus(PublishedStatus.Published)

  "filter" should "allow products slugs that are associated with a product" in {
    val (filter, celebrity) = filterWithMocksAndCelebrity
    val errorOrProduct = filter.filter(goodUrlSlug, celebrity)

    errorOrProduct should be(Right(productWithUrlSlug))
  }

  it should "not allow products slugs that are not associated with a product" in {
    val (filter, celebrity) = filterWithMocksAndCelebrity
    val errorOrProduct = filter.filter(badUrlSlug, celebrity)

    val result = errorOrProduct.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocksAndCelebrity: (RequireProductUrlSlug, Celebrity) = {
    val (productQueryFilters, celebrity) = mockProductQueryFiltersAndCelebrity
    (new RequireProductUrlSlug(productQueryFilters), celebrity)
  }

  private def mockProductQueryFiltersAndCelebrity = {
    val productQueryFilters = mock[ProductQueryFilters]

    val goodResponse = mock[FilterOneTable[Product]]
    val badResponse = mock[FilterOneTable[Product]]

    productQueryFilters.byUrlSlug(goodUrlSlug) returns goodResponse // Some(productWithUrlSlug)
    productQueryFilters.byUrlSlug(badUrlSlug) returns badResponse // None

    val goodQuery = mock[Query[Product]]
    goodQuery.headOption returns Some(productWithUrlSlug)

    val badQuery = mock[Query[Product]]
    badQuery.headOption returns None

    val celebrity = mock[Celebrity]
    celebrity.products(goodResponse) returns goodQuery
    celebrity.products(badResponse) returns badQuery

    (productQueryFilters, celebrity)
  }
}