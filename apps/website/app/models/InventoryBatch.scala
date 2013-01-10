package models

import com.google.inject.Inject
import services._
import db.{FilterOneTable, Schema, SavesWithLongKey, KeyedCaseClass}
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.util.Date
import org.squeryl.dsl.ManyToMany
import org.joda.time.DateTime
import enums.PublishedStatus

case class InventoryBatchServices @Inject()(store: InventoryBatchStore,
                                            orderStore: OrderStore,
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

  def getRemainingInventory: Int = {
    numInventory - services.orderStore.countOrders(List(id))
  }

  def hasInventory: Boolean = {
    getRemainingInventory > 0
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

class InventoryBatchStore @Inject()(schema: Schema, orderStore: OrderStore, inventoryBatchQueryFilters: InventoryBatchQueryFilters) extends SavesWithLongKey[InventoryBatch] with SavesCreatedUpdated[Long,InventoryBatch] {

  def findByCelebrity(celebrityId: Long, filters: FilterOneTable[InventoryBatch]*): Query[InventoryBatch] = {
    from(schema.inventoryBatches)((inventoryBatch) =>
      where(
        inventoryBatch.celebrityId === celebrityId and
          FilterOneTable.reduceFilters(filters, inventoryBatch)
      )
        select (inventoryBatch)
        orderBy (inventoryBatch.id asc)
    )
  }

  /**
   * Get available inventory batches by quantity and time for provided product.
   * (They are ordered by end date ascending.)
   */
  def getAvailableInventoryBatches(myProduct: Product): Iterable[InventoryBatch] = {
    val query = join(schema.inventoryBatches, schema.inventoryBatchProducts, schema.products, schema.orders.leftOuter)((inventoryBatch, association, product, order) =>
      where(
        product.id === myProduct.id and
          product._publishedStatus === PublishedStatus.Published.name and
          FilterOneTable.reduceFilters(List(inventoryBatchQueryFilters.activeOnly), inventoryBatch))
        groupBy (
          inventoryBatch.numInventory, inventoryBatch.id, order.map(o => o.id))
          //TODO: It would be great if this nvl would work instead of having to do this on the server.  Maybe a squeryl bug in here, idk.
          //          compute (sum(nvl(order.map(o => 1), 0)))
          on (
            inventoryBatch.id === association.inventoryBatchId,
            product.id === association.productId,
            inventoryBatch.id === order.map(_.inventoryBatchId))
          )

    val availableInventoryBatchIds = {
      val availableInventoryBatchIdsAndTotalInventoriesAndOrders = query.toList.map { row =>
        val totalInventory = row.key._1
        val inventoryBatchId = row.key._2
        val orderCount = row.key._3.map(_ => 1).getOrElse(0)
        (inventoryBatchId, totalInventory, orderCount)
      }

      val grouped = availableInventoryBatchIdsAndTotalInventoriesAndOrders.groupBy {
        case (inventoryBatchId, totalInventory, orderCount) => (inventoryBatchId, totalInventory)
      }
      val inventoryBatchIdAndRemainingInventories = grouped.map {
        case (inventoryBatchIdAndTotalInventory, everythingAndOrders) =>
          val (inventoryBatchId, totalInventory) = inventoryBatchIdAndTotalInventory
          everythingAndOrders.size match {
            case 1 => (inventoryBatchId, totalInventory - everythingAndOrders.head._3) // head._3 is 0 if there was no order on that inventory batch, otherwise it is 1
            case orderCount => (inventoryBatchId, totalInventory - orderCount)
          }
      }

      inventoryBatchIdAndRemainingInventories.filter {
        case (inventoryBatchId, remainingInventory) => (remainingInventory > 0)
      }
    }.map { case (inventoryBatchId, remainingInventory) => inventoryBatchId }

    val availableInventoryBatchQuery = from(schema.inventoryBatches)(inventoryBatch => select(inventoryBatch)orderBy(inventoryBatch.endDate asc)).
        withFilter(inventoryBatch => availableInventoryBatchIds.toList.contains(inventoryBatch.id))

    availableInventoryBatchQuery.map(ib => ib)
  }

  def inventoryBatches(product: Product): Query[InventoryBatch] with ManyToMany[InventoryBatch, InventoryBatchProduct] = {
    schema.inventoryBatchProducts.right(product)
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
