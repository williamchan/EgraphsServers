package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import libs.{Serialization, Time}

/**
 * Persistent entity representing the Orders made upon Products of our service
 */
case class Order(
  id: Long = 0,
  productId: Long = 0,
  buyerId: Long = 0,
  recipientId: Long = 0,
  verifiedEgraphId: Option[Long] = None,
  amountPaidInCents: Int = 0,
  messageToCelebrity: Option[String] = None,
  requestedMessage: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): Order = {
    Order.save(this)
  }

  /**
   * Renders the Order as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val buyer = Customer.findById(buyerId).get
    val recipient = if (buyerId != recipientId) Customer.findById(recipientId).get else buyer

    val requiredFields = Map(
      "id" -> id,
      "product.id" -> productId,
      "buyer.id" -> buyer.id,
      "buyer.name" -> buyer.name,
      "recipient.id" -> recipient.id,
      "recipient.name" -> recipient.name,
      "amountPaidInCents" -> amountPaidInCents
    )

    val optionalFields = Serialization.makeOptionalFieldMap(
      List(
        ("requestedMessage" -> requestedMessage),
        ("messageToCelebrity" -> messageToCelebrity)
      )
    )

    requiredFields ++ optionalFields
  }

  /**
   * Produces a new Egraph associated with this order.
   */
  def newEgraph(signature: Array[Byte], audio: Array[Byte]): Egraph = {
    Egraph(orderId=id, signature=signature, audio=audio).withState(AwaitingVerification)
  }
  
  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Order.unapply(this)
}

object Order extends Saves[Order] with SavesCreatedUpdated[Order] {
  //
  // Public Methods
  //
  def findByCelebrity(celebrityId: Long, filterFulfilled:Boolean = false):Iterable[Order] = {
    import Schema.{celebrities, products, orders}

    from(celebrities, products, orders)((celebrity, product, order) =>
      where(
        celebrity.id === celebrityId and
        celebrity.id === product.celebrityId and
        product.id === order.productId and
        (if (filterFulfilled) (order.verifiedEgraphId isNull) else (1 === 1))
      )
      select(order)
      orderBy(order.created asc)
    )
  }

  //
  // Saves[Order] methods
  //
  override val table = Schema.orders

  override def defineUpdate(theOld: Order, theNew: Order) = {
    updateIs(
      theOld.productId := theNew.productId,
      theOld.buyerId := theNew.buyerId,
      theOld.amountPaidInCents := theNew.amountPaidInCents,
      theOld.recipientId := theNew.recipientId,
      theOld.verifiedEgraphId := theNew.verifiedEgraphId,
      theOld.messageToCelebrity := theNew.messageToCelebrity,
      theOld.requestedMessage := theNew.requestedMessage,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Order] methods
  //
  override def withCreatedUpdated(toUpdate: Order, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
