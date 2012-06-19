package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class InventoryBatchTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[InventoryBatch]
with CreatedUpdatedEntityTests[InventoryBatch]
with ClearsDatabaseAndValidationBefore
with DBTransactionPerTest {
  private val inventoryBatchStore = AppConfig.instance[InventoryBatchStore]
  private val inventoryBatchQueryFilters = AppConfig.instance[InventoryBatchQueryFilters]

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
  "getExpectedDate" should "return a Date 7 days after endDate" in {
    newEntity.getExpectedDate should be(TestData.jan_08_2012)
  }

  "findByCelebrity" should "filter by activeOnly when composed with filter" in {
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

  "InventoryBatch" should "associate and dissociate with Products" in {
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

  "InventoryBatchProducts" should "be unique on inventoryBatchId and productId" in {
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

  "getActiveInventoryBatches" should "return InventoryBatches with active startDate-endDate periods" in {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    val inventoryBatch1 = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.today, endDate = TestData.tomorrow).save()
    val inventoryBatch2 = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.today, endDate = TestData.twoDaysHence).save()
    val inventoryBatchInThePast = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.jan_01_2012, endDate = TestData.feb_01_2012).save()
    val inventoryBatchInTheFuture = InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.tomorrow, endDate = TestData.twoDaysHence).save()

    inventoryBatchInThePast.products.associate(product)
    inventoryBatchInTheFuture.products.associate(product)
    inventoryBatchStore.getActiveInventoryBatches(product).toSeq.length should be(0)

    inventoryBatch1.products.associate(product)
    inventoryBatchStore.getActiveInventoryBatches(product).toSet should be(Set(inventoryBatch1))

    inventoryBatch2.products.associate(product)
    inventoryBatchStore.getActiveInventoryBatches(product).toSet should be(Set(inventoryBatch1, inventoryBatch2))
  }

  "selectAvailableInventoryBatch" should "return inventoryBatch with earliest endDate that has available inventory" in {
    val celebrity = TestData.newSavedCelebrity()
    val ib1 = TestData.newSavedInventoryBatch(celebrity).copy(numInventory = 0, endDate = TestData.tomorrow).save()
    val ib2 = TestData.newSavedInventoryBatch(celebrity).copy(numInventory = 1, endDate = TestData.twoDaysHence).save()
    val ib3 = TestData.newSavedInventoryBatch(celebrity).copy(numInventory = 1, endDate = TestData.threeDaysHence).save()
    val ib4 = TestData.newSavedInventoryBatch(celebrity).copy(numInventory = 1, endDate = TestData.sevenDaysHence).save()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity)
    ib2.products.associate(product)
    TestData.newSavedOrder(Some(product)).copy(inventoryBatchId = ib2.id).save()

    // zero case
    inventoryBatchStore.selectAvailableInventoryBatch(List.empty[InventoryBatch]) should be(None)

    // 1 case
    inventoryBatchStore.selectAvailableInventoryBatch(List(ib3)) should be(Some(ib3))

    // multiple case: ib1 and ib2 should be skipped because they lack inventory, and ib3 should be sorted to come before ib4
    inventoryBatchStore.selectAvailableInventoryBatch(List(ib1, ib2, ib4, ib3)) should be(Some(ib3))
  }
}
