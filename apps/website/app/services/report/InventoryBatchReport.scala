package services.report

import com.google.inject.Inject
import services.db.Schema
import java.io.File
import org.squeryl.PrimitiveTypeMode._
import org.joda.time.DateTime
import services.Time
import org.squeryl.dsl.GroupWithMeasures
import org.squeryl.Query

class InventoryBatchReport @Inject()(schema: Schema) extends Report {

  override val reportName = "inventory-batch-report"

  def report(): File = {
    val oneMonthAgo = new DateTime().minusDays(30).toDate
    import schema.{inventoryBatches, celebrities}
    val batchesAndCelebrityName = from(inventoryBatches, celebrities)((inventoryBatch, celebrity) =>
      where((inventoryBatch.startDate between(oneMonthAgo, Time.today)) and
        inventoryBatch.celebrityId === celebrity.id)
        select (inventoryBatch, celebrity.publicName)
        orderBy (inventoryBatch.id asc)
    )

    // This is useful for calculating inventory remaining.
    val batchIdsAndOrderCount = getOrderCountByBatchId(batchesAndCelebrityName.map(b => b._1.id).toList)

    val headerLine = csvLine("inventoryBatchId", "startDate", "endDate", "numInventory", "remainingInventory", "celebrityId", "celebrityPublicName")
    val csv = new StringBuilder(headerLine)
    for (batchAndCelebrityName <- batchesAndCelebrityName) {
      val batch = batchAndCelebrityName._1
      csv.append(csvLine(
        batch.id,
        batch.startDate,
        batch.endDate,
        batch.numInventory,
        batch.numInventory - batchIdsAndOrderCount.get(batch.id).getOrElse(0),
        batch.celebrityId,
        batchAndCelebrityName._2.getOrElse("")
      ))
    }
    csvFile(csv)
  }

  private def getOrderCountByBatchId(batchIds: List[Long]): Map[Long, Int] = {
    val batchIdsAndOrderCountQuery: Query[GroupWithMeasures[Long, Long]] =
      from(schema.orders)(order =>
        where(order.inventoryBatchId in batchIds)
          groupBy (order.inventoryBatchId)
          compute (countDistinct(order.id))
      )
    (for (r <- batchIdsAndOrderCountQuery) yield (r.key, r.measures.toInt)).toMap
  }
}
