package models

import utils._
import services.AppConfig

class InventoryBatchTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[InventoryBatch]
  with CreatedUpdatedEntityTests[Long, InventoryBatch]
  with DateShouldMatchers
  with DBTransactionPerTest 
{

  private def inventoryBatchStore = AppConfig.instance[InventoryBatchStore]
  private def inventoryBatchQueryFilters = AppConfig.instance[InventoryBatchQueryFilters]

  //
  // SavingEntityTests[InventoryBatch] methods
  //
  override def newEntity = {
    val celebrity = TestData.newSavedCelebrity()
    InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.jan_01_2012, endDate = TestData.jan_01_2012)
  }

  override def saveEntity(toSave: InventoryBatch) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    inventoryBatchStore.findById(id)
  }

  override def transformEntity(toTransform: InventoryBatch) = {
    toTransform.copy(
      numInventory = 25)
  }

  //
  // Test cases
  //
  "findByCelebrity" should "filter by activeOnly when composed with filter" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()

    var inventoryBatch = TestData.newSavedInventoryBatch(celebrity = celebrity)
    inventoryBatchStore.findByCelebrity(celebrityId = celebrity.id, inventoryBatchQueryFilters.activeOnly).toSet should be(Set(inventoryBatch))

    inventoryBatch = inventoryBatch.copy(startDate = TestData.today, endDate = TestData.today).save()
    inventoryBatchStore.findByCelebrity(celebrityId = celebrity.id, inventoryBatchQueryFilters.activeOnly).toSet should be(Set(inventoryBatch))

    inventoryBatch.copy(startDate = TestData.jan_01_2012, endDate = TestData.feb_01_2012).save()
    inventoryBatchStore.findByCelebrity(celebrityId = celebrity.id, inventoryBatchQueryFilters.activeOnly).toSeq.length should be(0)

    inventoryBatch.copy(startDate = TestData.tomorrow, endDate = TestData.tomorrow).save()
    inventoryBatchStore.findByCelebrity(celebrityId = celebrity.id, inventoryBatchQueryFilters.activeOnly).toSeq.length should be(0)
  }

  "InventoryBatch" should "associate and dissociate with Products" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val inventoryBatch = TestData.newSavedInventoryBatch(celebrity = celebrity)

    val product1 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product2 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product3 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)

    inventoryBatch.products.associate(product1)
    inventoryBatch.products.toSet should be(Set(product1))

    inventoryBatch.products.associate(product2)
    inventoryBatch.products.associate(product3)
    inventoryBatch.products.toSet should be(Set(product1, product2, product3))

    inventoryBatch.products.dissociate(product3)
    inventoryBatch.products.toSet should be(Set(product1, product2))

    inventoryBatch.products.dissociateAll
    inventoryBatch.products.toSeq.length should be(0)
  }

  "InventoryBatchProducts" should "be unique on inventoryBatchId and productId" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val inventoryBatch = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)

    inventoryBatch.products.associate(product)
    inventoryBatch.products.toSeq.length should be(1)

    val exception = intercept[RuntimeException] {
      inventoryBatch.products.associate(product)
    }
    val psqlException = exception.getCause
    psqlException.getClass.getCanonicalName should be("org.postgresql.util.PSQLException")
    psqlException.getLocalizedMessage should startWith("ERROR: duplicate key value violates unique constraint \"idxd8791317\"")
  }

  "getAvailableInventoryBatches" should "return InventoryBatches with active startDate-endDate periods" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    val inventoryBatch1 = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.today, endDate = TestData.tomorrow).save()
    val inventoryBatch2 = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.today, endDate = TestData.twoDaysHence).save()
    val inventoryBatchInThePast = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.jan_01_2012, endDate = TestData.feb_01_2012).save()
    val inventoryBatchInTheFuture = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.tomorrow, endDate = TestData.twoDaysHence).save()

    inventoryBatchInThePast.products.associate(product)
    inventoryBatchInTheFuture.products.associate(product)
    inventoryBatchStore.getAvailableInventoryBatches(product).isEmpty should be(true)

    inventoryBatch1.products.associate(product)
    inventoryBatchStore.getAvailableInventoryBatches(product).toSet should be(Set(inventoryBatch1))

    inventoryBatch2.products.associate(product)
    inventoryBatchStore.getAvailableInventoryBatches(product).toSet should be(Set(inventoryBatch1, inventoryBatch2))
  }

  it should "not return inventory batches with no inventory quantity" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    val ib = TestData.newSavedInventoryBatch(product).copy(numInventory = 0, endDate = TestData.tomorrow).save()

    // zero case
    inventoryBatchStore.getAvailableInventoryBatches(product).isEmpty should be(true)
  }

  it should "not return inventoryBatches that have been sold out" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    val ib1 = TestData.newSavedInventoryBatch(product).copy(numInventory = 1, endDate = TestData.threeDaysHence).save()
    val ib2 = TestData.newSavedInventoryBatch(product).copy(numInventory = 1, endDate = TestData.sevenDaysHence).save()
    TestData.newSavedOrder(Some(product)).copy(inventoryBatchId = ib1.id).save()

    // product associated with ib1 and ib2, but ib1 should have 0 inventory left, so only should have available ib2.
    inventoryBatchStore.getAvailableInventoryBatches(product).toSet should be(Set(ib2))
  }

  it should "return inventoryBatches that have remaining inventory quantities" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    val ib1 = TestData.newSavedInventoryBatch(product).copy(numInventory = 1, endDate = TestData.threeDaysHence).save()
    val ib2 = TestData.newSavedInventoryBatch(product).copy(numInventory = 2, endDate = TestData.sevenDaysHence).save()
    val ib3 = TestData.newSavedInventoryBatch(product).copy(numInventory = 0, endDate = TestData.tomorrow).save()

    // ordered by date
    inventoryBatchStore.getAvailableInventoryBatches(product).toList should be(List(ib1, ib2))
  }

  it should "return inventoryBatches that have correct date ordering" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    val ib1 = TestData.newSavedInventoryBatch(product).copy(numInventory = 1, endDate = TestData.threeDaysHence).save()
    val ib2 = TestData.newSavedInventoryBatch(product).copy(numInventory = 1, endDate = TestData.sevenDaysHence).save()
    val ib3 = TestData.newSavedInventoryBatch(product).copy(numInventory = 1, endDate = TestData.tomorrow).save()

    // ordered by date
    inventoryBatchStore.getAvailableInventoryBatches(product).toList should be(List(ib3, ib1, ib2))
  }
}
