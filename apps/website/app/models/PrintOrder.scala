package models

import com.google.inject.Inject
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{Saves, Schema, KeyedCaseClass}

case class PrintOrderServices @Inject() (store: PrintOrderStore, orderStore: OrderStore)

/**
 * Persistent entity representing the Orders made upon Products of our service
 */

case class PrintOrder(id: Long = 0,
                      orderId: Long = 0,
                      shippingAddress: String = "",
                      quantity: Int = 1,
                      isFulfilled: Boolean = false,
                      created: Timestamp = Time.defaultTimestamp,
                      updated: Timestamp = Time.defaultTimestamp,
                      services: PrintOrderServices = AppConfig.instance[PrintOrderServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated
{
  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): PrintOrder = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = PrintOrder.unapply(this)
}

class PrintOrderStore @Inject() (schema: Schema) extends Saves[PrintOrder] with SavesCreatedUpdated[PrintOrder] {
  import org.squeryl.PrimitiveTypeMode._
  //
  // Saves[PrintOrder] methods
  //
  override val table = schema.printOrders

  override def defineUpdate(theOld: PrintOrder, theNew: PrintOrder) = {
    updateIs(
      theOld.orderId := theNew.orderId,
      theOld.shippingAddress := theNew.shippingAddress,
      theOld.quantity := theNew.quantity,
      theOld.isFulfilled := theNew.isFulfilled,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }
  //
  // SavesCreatedUpdated[PrintOrder] methods
  //
  override def withCreatedUpdated(toUpdate: PrintOrder, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
