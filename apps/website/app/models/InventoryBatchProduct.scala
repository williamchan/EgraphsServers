package models

import services._
import db._
import com.google.inject.Inject

case class InventoryBatchProductServices @Inject()(store: InventoryBatchProductStore)

case class InventoryBatchProduct(
                                  id: Long = 0L,
                                  inventoryBatchId: Long = 0L,
                                  productId: Long = 0L,
                                  services: InventoryBatchProductServices = AppConfig.instance[InventoryBatchProductServices]
                                  ) extends KeyedCaseClass[Long] {

  //
  // Public members
  //
  def save(): InventoryBatchProduct = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = {
    InventoryBatchProduct.unapply(this)
  }
}

class InventoryBatchProductStore @Inject()(schema: Schema) extends Saves[InventoryBatchProduct] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Saves methods
  //
  def table = schema.inventoryBatchProducts

  override def defineUpdate(theOld: InventoryBatchProduct, theNew: InventoryBatchProduct) = {
    updateIs(
      theOld.inventoryBatchId := theNew.inventoryBatchId,
      theOld.productId := theNew.productId
    )
  }
}
