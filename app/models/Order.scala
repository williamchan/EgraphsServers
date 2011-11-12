package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import libs.{Serialization, Time}
import org.squeryl.dsl.ast.{LogicalBoolean, PostfixOperatorNode, BinaryOperatorNodeLogicalBoolean, EqualityExpression}

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

    requiredFields ++ optionalFields ++ renderCreatedUpdatedForApi
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
  private def reduceFilters(
    filters: Iterable[FindByCelebrityFilter],
    celebrity: Celebrity,
    product: Product,
    order: Order): LogicalBoolean =
  {
    filters.headOption match {
      // Just return the trivial comparison if there were no filters
      case None =>
        (1 === 1)

      // Otherwise reduce the collection of filters against the first
      case Some(firstFilter) =>
        filters.tail.foldLeft(firstFilter.test(celebrity, product, order))(
          (compositeFilter, nextFilter) =>
            (nextFilter.test(celebrity, product, order) and compositeFilter)
        )
    }
  }

  def findByCelebrity(celebrityId: Long, filters: FindByCelebrityFilter*):Iterable[Order] = {
    import Schema.{celebrities, products, orders}

    from(celebrities, products, orders)((celebrity, product, order) =>
      where(
        celebrity.id === celebrityId and
        celebrity.id === product.celebrityId and
        product.id === order.productId and
        reduceFilters(filters, celebrity, product, order)
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

/**
 * Base definition of an object that can apply a Squeryl filter against the join
 * between Schema.celebrities, Schema.products, Schema.orders that occurs in the
 * Order.findByCelebrity function.
 */
trait FindByCelebrityFilter {
  def test(celebrity: Celebrity, product: Product, order: Order): LogicalBoolean
}

/**
 * Returns only orders that haven't been successfully fulfilled
 */
object UnfulfilledFilter extends FindByCelebrityFilter {
  override def test(celebrity: Celebrity, product: Product, order: Order) = {
    (order.verifiedEgraphId isNull)
  }
}

/**
 * Returns only orders with the given orderId.
 */
case class OrderIdFilter(id: Long) extends FindByCelebrityFilter {
  override def test(celebrity: Celebrity, product: Product, order: Order) = {
    (order.id === id)
  }
}
