package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.Product
import models.ProductStore
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import utils.EgraphsUnitTest
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status

@RunWith(classOf[JUnitRunner])
class RequireProductIdTests extends EgraphsUnitTest {
  val goodId = 309812309808L
  val badId = 1329812398123L

  val product = Product(services = null)

  "filter" should "allow product ids that are associated with an product" in {
    val errorOrProduct = filterWithMocks.filter(goodId)

    errorOrProduct should be(Right(product))
  }

  it should "not allow product ids that are not associated with an product" in {
    val errorOrProduct = filterWithMocks.filter(badId)
    val result = errorOrProduct.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks: RequireProductId = {
    new RequireProductId(mockProductStore)
  }

  private def mockProductStore = {
    import services.db.Schema
    import models.InventoryBatchQueryFilters

    class MockableProductStore(schema: Schema, inventoryBatchQueryFilters: InventoryBatchQueryFilters) extends ProductStore(schema, inventoryBatchQueryFilters) {
      override def findById(id: Long): Option[Product] = { None }
    }

    val productStore = mock[MockableProductStore]

    productStore.findById(goodId) returns Some(product)
    productStore.findById(badId) returns None

    productStore
  }
}