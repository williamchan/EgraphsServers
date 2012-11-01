package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.InventoryBatch
import models.InventoryBatchStore
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireInventoryBatchIdTests extends EgraphsUnitTest {
  val goodId = 309812309808L
  val badId = 1329812398123L

  val inventoryBatch = InventoryBatch(services = null)

  "filter" should "allow inventoryBatch ids that are associated with an inventoryBatch" in {
    val errorOrInventoryBatch = filterWithMocks.filter(goodId)

    errorOrInventoryBatch should be(Right(inventoryBatch))
  }

  it should "not allow inventoryBatch ids that are not associated with an inventoryBatch" in {
    val errorOrInventoryBatch = filterWithMocks.filter(badId)
    val result = errorOrInventoryBatch.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks: RequireInventoryBatchId = {
    new RequireInventoryBatchId(mockInventoryBatchStore)
  }

  private def mockInventoryBatchStore = {
    import services.db.Schema
    import models.InventoryBatchQueryFilters
    import models.OrderStore

    class MockableInventoryBatchStore(schema: Schema, orderStore: OrderStore, inventoryBatchQueryFilters: InventoryBatchQueryFilters) extends InventoryBatchStore(schema, orderStore, inventoryBatchQueryFilters) {
      override def findById(id: Long): Option[InventoryBatch] = { None }
    }

    val inventoryBatchStore = mock[MockableInventoryBatchStore]

    inventoryBatchStore.findById(goodId) returns Some(inventoryBatch)
    inventoryBatchStore.findById(badId) returns None

    inventoryBatchStore
  }
}