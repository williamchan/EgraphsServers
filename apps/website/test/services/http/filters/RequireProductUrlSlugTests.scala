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
import services.AppConfig
import utils.TestData
import utils.DBTransactionPerTest
import org.apache.commons.lang3.RandomStringUtils

@RunWith(classOf[JUnitRunner]) //If you ever have to look at this.  Maybe rewrite as an integration test for readability.
class RequireProductUrlSlugTests extends EgraphsUnitTest with DBTransactionPerTest {
  val goodUrlSlug = "goodSlug"
  val badUrlSlug = "badSlug"

  val productWithUrlSlug = Product(services = null).withPublishedStatus(PublishedStatus.Published)

  "filter" should "allow products slugs that are associated with a product" in {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProduct(Some(celebrity))

    val errorOrProduct = filter.filter(product.urlSlug, celebrity)

    errorOrProduct should be(Right(product))
  }

  it should "not allow products slugs that are not associated with a product" in {
    val celebrity = TestData.newSavedCelebrity()
    val productSlug = RandomStringUtils.random(20)

    val errorOrProduct = filter.filter(productSlug, celebrity)

    val result = errorOrProduct.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filter: RequireProductUrlSlug = {
    AppConfig.instance[RequireProductUrlSlug] 
  }
}