package models

import com.google.inject.Inject
import services._
import db.{FilterOneTable, Schema, Saves, KeyedCaseClass}
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.util.Date
import org.squeryl.dsl.ManyToMany

case class InventoryBatchServices @Inject()(store: InventoryBatchStore,
                                            productStore: ProductStore,
                                            celebStore: CelebrityStore)

case class InventoryBatch(
                           id: Long = 0L,
                           celebrityId: Long = 0L,
                           numInventory: Int = 0,
                           startDate: Date = Time.today,
                           endDate: Date = Time.today,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: InventoryBatchServices = AppConfig.instance[InventoryBatchServices]
                           ) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  lazy val products = services.productStore.products(this)

  def celebrity: Celebrity = {
    services.celebStore.get(celebrityId)
  }

  //
  // Public members
  //
  def save(): InventoryBatch = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = {
    InventoryBatch.unapply(this)
  }

}

class InventoryBatchStore @Inject()(schema: Schema, orderStore: OrderStore, inventoryBatchQueryFilters: InventoryBatchQueryFilters) extends Saves[InventoryBatch] with SavesCreatedUpdated[InventoryBatch] {

  def findByCelebrity(celebrityId: Long, filters: FilterOneTable[InventoryBatch]*): Query[InventoryBatch] = {
    from(schema.inventoryBatches)((inventoryBatch) =>
      where(
        inventoryBatch.celebrityId === celebrityId and
          FilterOneTable.reduceFilters(filters, inventoryBatch)
      )
        select (inventoryBatch)
        orderBy (inventoryBatch.created asc)
    )
  }

  def getActiveInventoryBatches(product: Product): Query[InventoryBatch] = {
    from(schema.inventoryBatches, schema.inventoryBatchProducts)((inventoryBatch, association) =>
      where(
        inventoryBatch.id === association.inventoryBatchId
          and association.productId === product.id
          and FilterOneTable.reduceFilters(List(inventoryBatchQueryFilters.activeOnly), inventoryBatch)
      )
        select (inventoryBatch)
    )
  }

  def inventoryBatches(product: Product): Query[InventoryBatch] with ManyToMany[InventoryBatch, InventoryBatchProduct] = {
    schema.inventoryBatchProducts.right(product)
  }

  /**
   * @param activeInventoryBatches inventoryBatches that are active based on startDate and endDate
   * @return the inventoryBatch that has the earliest endDate and has inventory remaining (numInventory > number of associated orders)
   */
  def selectAvailableInventoryBatch(activeInventoryBatches: Seq[InventoryBatch]): Option[InventoryBatch] = {
    if (activeInventoryBatches.headOption.isDefined && activeInventoryBatches.tail.headOption.isEmpty) {
      // There is only one inventoryBatch. Assume that it is the correct inventoryBatch against which to order.
      activeInventoryBatches.headOption

    } else {
      for (inventoryBatch <- activeInventoryBatches.sortWith((batch1, batch2) => batch1.endDate.before(batch2.endDate))) {
        if (orderStore.countOrders(List(inventoryBatch.id)) < inventoryBatch.numInventory) {
          return Some(inventoryBatch)
        }
      }
      None
    }
  }

  //
  // Saves methods
  //
  def table = schema.inventoryBatches

  override def defineUpdate(theOld: InventoryBatch, theNew: InventoryBatch) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.numInventory := theNew.numInventory,
      theOld.startDate := theNew.startDate,
      theOld.endDate := theNew.endDate,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated methods
  //
  override def withCreatedUpdated(toUpdate: InventoryBatch, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


class InventoryBatchQueryFilters @Inject()(schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  def activeOnly: FilterOneTable[InventoryBatch] = {
    new FilterOneTable[InventoryBatch] {
      override def test(inventoryBatch: InventoryBatch) = {
        Time.today between(inventoryBatch.startDate, inventoryBatch.endDate)
      }
    }
  }
}
