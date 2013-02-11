package models

import enums._
import frontend.egraphs.{OrderDetails, PendingEgraphViewModel, FulfilledEgraphViewModel}
import java.sql.Timestamp
import java.util.Date
import org.joda.money.Money
import services.db.{FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey}
import services.Finance.TypeConversions._
import services._
import blobs.AccessPolicy
import com.google.inject._
import mail.TransactionalMail
import payment.{Charge, Payment}
import org.squeryl.Query
import org.apache.commons.mail.HtmlEmail
import com.google.inject.Inject
import java.text.SimpleDateFormat
import controllers.website.consumer.StorefrontChoosePhotoConsumerEndpoints
import social.{Twitter, Facebook}
import play.api.mvc.RequestHeader
import play.api.templates.Html
import db.Deletes
import java.sql.Connection
import services.db.CurrentTransaction
import org.joda.time.DateMidnight
import org.apache.commons.lang3.time.DateUtils
import java.util.Calendar
import org.joda.time.DateTime
import controllers.api.FulfilledOrderBundle
import models.frontend.email.{RegularEgraphSignedEmailViewModel, GiftEgraphSignedEmailViewModel}
import services.email.EgraphSignedEmailPreparer

case class OrderServices @Inject() (
  store: OrderStore,
  customerStore: CustomerStore,
  celebrityStore: CelebrityStore,
  inventoryStore: InventoryBatchStore,
  productStore: ProductStore,
  printOrderStore: PrintOrderStore,
  payment: Payment,
  mail: TransactionalMail,
  cashTransactionStore: CashTransactionStore,
  egraphServices: Provider[EgraphServices],
  consumerApp: ConsumerApplication,
  @CurrentTransaction connectionFactory: () => Connection
)

object Order {
  def defaultExpectedDate: Date = new DateMidnight().plusDays(30).toDate

  /**
   * Gets the Date with delay applied to the current day, rounded up to the nearest day.
   */
  def expectedDateFromDelay(delayInMinutes: Int): Date = {
    addMinutesAndRoundUpToNextDay(DateTime.now(), delayInMinutes)
  }

  def addMinutesAndRoundUpToNextDay(date: DateTime, minutesToAdd: Int): Date = {
    val dateSum = date.plusMinutes(minutesToAdd).toDate

    // Make sure to round up to nearest day
    val truncatedDate = DateUtils.truncate(dateSum, Calendar.DATE)
    if(DateUtils.isSameInstant(truncatedDate, dateSum)) {
      truncatedDate
    } else {
      //round up
      DateUtils.addDays(truncatedDate, 1)
    }
  }

  /**
   * Gets you the expected delivery date of the celebrity. 
   */
  def expectedDeliveryDate(celebrity: Celebrity): Date = {
    Order.expectedDateFromDelay(celebrity.expectedOrderDelayInMinutes)
  }
}

/**
 * Persistent entity representing the Orders made upon Products of our service
 */
case class Order(
  id: Long = 0,
  productId: Long = 0,
  inventoryBatchId: Long = 0,
  buyerId: Long = 0,
  recipientId: Long = 0,
  recipientName: String = "",
  _paymentStatus: String = PaymentStatus.NotCharged.name,
  _reviewStatus: String = OrderReviewStatus.PendingAdminReview.name,
  rejectionReason: Option[String] = None,
  _privacyStatus: String = PrivacyStatus.Public.name,
  _writtenMessageRequest: String = WrittenMessageRequest.SpecificMessage.name,
  amountPaidInCurrency: BigDecimal = 0,
  messageToCelebrity: Option[String] = None,
  requestedMessage: Option[String] = None,
  expectedDate: Date = Order.defaultExpectedDate,
  _orderType: String = OrderType.Normal.name,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: OrderServices = AppConfig.instance[OrderServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasPrivacyStatus[Order]
  with HasPaymentStatus[Order]
  with HasOrderReviewStatus[Order]
  with HasOrderType[Order]
  with HasWrittenMessageRequest[Order]
{
  
  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): Order = {
    require(!recipientName.isEmpty, "Order: recipientName must be specified")

    services.store.save(this)
  }

  def amountPaid: Money = {
    amountPaidInCurrency.toMoney()
  }

  /** Retrieves the purchasing Customer from the database. */
  def buyer: Customer = {
    services.customerStore.get(buyerId)
  }

  /** Retrieves the receiving Customer from the database */
  def recipient: Customer = {
    services.customerStore.get(recipientId)
  }

  /** Retrieves the purchased Product from the database */
  def product: Product = {
    services.productStore.get(productId)
  }

  /** Retrieves the inventory batch against which the order was made */
  def inventoryBatch: InventoryBatch = {
    services.inventoryStore.get(inventoryBatchId)
  }

  def isPublic = {
    privacyStatus == PrivacyStatus.Public
  }

  def isPromotional = {
    _orderType == OrderType.Promotional.name
  }

  def redactedRecipientName: String = {
    val nameParts = recipientName.trim.split("( )|(-)").toList

    val firstName = nameParts.head
    val lastNames = nameParts.tail.filter(thePart => thePart != "")
    val lastNamesRedacted = if (lastNames.isEmpty) {
      ""
    } else {
      lastNames.map(theName => theName(0)).mkString(" ", ".", ".")
    }

    firstName + lastNamesRedacted
  }

  /**
   * Refund functionality is ready to be called, but is not exposed anywhere.
   * Hopefully we will not have many refund requests when we launch.
   */
  def refund(): (Order, Charge) = {
    // Do a bunch of validation to make sure we have everything to do this refund
    require(paymentStatus == PaymentStatus.Charged, "Refunding an Order requires that the Order be already Charged")
    val maybeCashTransaction = services.cashTransactionStore.findByOrderId(id).headOption

    require(maybeCashTransaction.isDefined && maybeCashTransaction.get.stripeChargeId.isDefined, "Refunding an Order requires that the Order be already Charged")
    val cashTransaction = maybeCashTransaction.get
    val stripeChargeId = cashTransaction.stripeChargeId.get

    // Do the refund and exception handling
    val maybeCustomer = services.customerStore.findById(buyerId)
    val errorStringOrRefundedOrderAndCharge = for (
      customer <- maybeCustomer.toRight("There is no customer!").right
    ) yield {
      doRefund(customer, stripeChargeId)
    }

    errorStringOrRefundedOrderAndCharge.fold(
      (errorString) => throw new Exception(errorString),
      (orderAndCharge) => orderAndCharge
    )
  }

  private def doRefund(buyer: Customer, stripeChargeId: String): (Order, Charge) = {
    val refundedCharge = services.payment.refund(stripeChargeId)
    CashTransaction(accountId = buyer.account.id, orderId = Some(id))
      .withCash(amountPaid.negated())
      .withCashTransactionType(CashTransactionType.PurchaseRefund)
      .save()
    val refundedOrder = withPaymentStatus(PaymentStatus.Refunded).save()
    (refundedOrder, refundedCharge)
  }

  /**
   * Rejects this order and creates a new order that is a copy to take it's place.
   * The copy has a new product and inventory batch and reflects the new price.
   */
  def rejectAndCreateNewOrderWithNewProduct(newProduct: Product, newInventoryBatch: InventoryBatch): Order = {
    // create a new order only need to fill in mandatory fields now, just to get a new id.
    val newOrderUnsaved = withReviewStatus(OrderReviewStatus.PendingAdminReview)
      .copy(
        productId = newProduct.id,
        rejectionReason = None,
        inventoryBatchId = newInventoryBatch.id,
        amountPaidInCurrency = newProduct.priceInCurrency
      )
    val newOrder = services.store.insert(newOrderUnsaved)

    // mark old order invalid
    val oldOrder = this.withReviewStatus(OrderReviewStatus.RejectedByAdmin).copy(rejectionReason = Some("Changed product to " + newProduct.id + " with new order " + newOrder.id)).save()
    // update other objects that care about the old order, since they shouldn't anymore
    services.cashTransactionStore.findByOrderId(oldOrder.id).foreach(transaction => transaction.copy(orderId = Some(newOrder.id)).save())
    // NOTE: if print order prices become different for each product, this will need to reflect that, but isn't now.
    services.printOrderStore.findByOrderId(oldOrder.id).foreach(printOrder => printOrder.copy(orderId = newOrder.id).save())

    // if we can delete the old one, that means there are no dangling foreign keys.  otherwise we have a problem and should roll back.
    // we don't actually want to delete the old order though, so we will save it after we delete it.
    val connection = services.connectionFactory()
    val savepoint = connection.setSavepoint()
    services.store.delete(oldOrder)
    connection.rollback(savepoint)

    newOrder
  }

  def approveByAdmin(admin: Administrator): Order = {
    require(admin != null, "Must be approved by an Administrator")
    require(reviewStatus == OrderReviewStatus.PendingAdminReview, "Must be PendingAdminReview before approving by admin")
    withReviewStatus(OrderReviewStatus.ApprovedByAdmin)
  }

  def rejectByAdmin(admin: Administrator, rejectionReason: Option[String] = None): Order = {
    require(admin != null, "Must be rejected by an Administrator")
    require(reviewStatus == OrderReviewStatus.PendingAdminReview, "Must be PendingAdminReview before rejecting by admin")
    withReviewStatus(OrderReviewStatus.RejectedByAdmin).copy(rejectionReason = rejectionReason)
  }

  def rejectByCelebrity(celebrity: Celebrity, rejectionReason: Option[String] = None): Order = {
    require(celebrity != null, "Must be rejected by Celebrity associated with this Order")
    require(celebrity.id == product.celebrityId, "Must be rejected by Celebrity associated with this Order")
    require(reviewStatus == OrderReviewStatus.ApprovedByAdmin, "Must be ApprovedByAdmin before rejecting by celebrity")
    withReviewStatus(OrderReviewStatus.RejectedByCelebrity).copy(rejectionReason = rejectionReason)
  }

  def sendEgraphSignedMail[A](implicit request: RequestHeader) {
    val (email, htmlMsg, textMsg) = prepareEgraphSignedEmail
    services.mail.send(email, Some(textMsg), Some(htmlMsg))
  }
  
  // This function provides a hook for testing the email
  def prepareEgraphSignedEmail
  : (HtmlEmail, Html, String)  = 
  {
    val (viewEgraphUrl, celebrity, email, coupon) = EgraphSignedEmailPreparer.prepareEgraphSignedEmailHelper(this, services)

    val egraphSignedEmailStack = if (buyerId == recipientId) {
      RegularEgraphSignedEmailViewModel(viewEgraphUrl, celebrity.publicName, this.recipientName, coupon.discountAmount.toInt, coupon.code)
    } else {
      GiftEgraphSignedEmailViewModel(viewEgraphUrl, celebrity.publicName, this.recipientName, coupon.discountAmount.toInt, coupon.code, this.buyer.name)
    }

    val (htmlMsg, textMsg) = EgraphSignedEmailPreparer.getHtmlAndTextMsgs(egraphSignedEmailStack)
    (email, htmlMsg, textMsg)
  }

  /**
   * Renders the Order as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val customerStore = services.customerStore
    val buyer = customerStore.get(buyerId)
    val recipient = if (buyerId != recipientId) customerStore.get(recipientId) else buyer

    // Alias CelebrityChoosesMessage to SpecificMessage for now -- iPad doesn't need to know
    // the difference.
    val writtenMessageRequestToWrite = writtenMessageRequest match {
      case WrittenMessageRequest.CelebrityChoosesMessage =>
        WrittenMessageRequest.SpecificMessage

      case otherValue =>
        otherValue
    }

    val requiredFields = Map(
      "id" -> id,
      "product" -> product.renderedForApi,
      "buyerId" -> buyer.id,
      "buyerName" -> buyer.name,
      "recipientId" -> recipient.id,
      "recipientName" -> recipientName,
      "amountPaidInCents" -> amountPaid.getAmountMinor,
      "reviewStatus" -> reviewStatus.name,
      "audioPrompt" -> ("Recipient: " + recipientName),
      "orderType" -> writtenMessageRequestToWrite.name     // Oops, this should be more accurately named writtenMessageType
    )

    val optionalFields = Utils.makeOptionalFieldMap(
      List(
        "requestedMessage" -> writtenMessageRequestText,
        "messageToCelebrity" -> messageToCelebrity
      )
    )

    requiredFields ++ optionalFields ++ renderCreatedUpdatedForApi
  }

  /**
   * Produces a new Egraph associated with this order.
   */
  def newEgraph: Egraph = {
    Egraph(orderId=id, services=services.egraphServices.get)
  }

  def isBuyerOrRecipient(customerId: Option[Long]): Boolean = {
    customerId match {
      case None => false
      case Some(custId) => buyerId == custId || recipientId == custId
    }
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Order.unapply(this)

  override def withOrderType(status: OrderType.EnumVal) = {
    this.copy(_orderType = status.name)
  }

  override def withPrivacyStatus(status: PrivacyStatus.EnumVal) = {
    this.copy(_privacyStatus = status.name)
  }

  override def withPaymentStatus(status: PaymentStatus.EnumVal) = {
    this.copy(_paymentStatus = status.name)
  }

  override def withReviewStatus(status: OrderReviewStatus.EnumVal) = {
    this.copy(_reviewStatus = status.name)
  }

  def withWrittenMessageRequest(enum: WrittenMessageRequest) = {
    this.copy(_writtenMessageRequest = enum.name)
  }

  //
  // Private members
  //
  private def writtenMessageRequestText:Option[String] = {
    writtenMessageRequest match {
      case WrittenMessageRequest.SignatureOnly =>
        None

      case WrittenMessageRequest.CelebrityChoosesMessage if requestedMessage.isEmpty =>
        Some("[Make up a message and write it!]")

      case WrittenMessageRequest.CelebrityChoosesMessage if requestedMessage.isDefined =>
        requestedMessage

      case WrittenMessageRequest.SpecificMessage =>
        requestedMessage
    }
  }
}

case class FulfilledOrder(order: Order, egraph: Egraph)

/** Thin semantic wrapper around a tuple for product order and egraph */
case class FulfilledProductOrder(product: Product, order:Order, egraph: Egraph)

class OrderStore @Inject() (
  schema: Schema
) extends SavesWithLongKey[Order]
  with SavesCreatedUpdated[Order]
  with Deletes[Long, Order]
{
  import org.squeryl.PrimitiveTypeMode._
  //
  // Public methods
  //
  /**
   * Returns a completed Order/Egraph combination with the given ID
   */
  def findFulfilledWithId(id: Long): Option[FulfilledOrder] = {
    import schema.{orders, egraphs}
    from(orders, egraphs)((order, egraph) =>
      where(
        order.id === id and
          egraph.orderId === order.id and
          egraph._egraphState === EgraphState.Published.name
      )
        select (FulfilledOrder(order, egraph))
    ).headOption
  }

  def findFulfilledForCustomer(customer: Customer): Iterable[FulfilledOrderBundle] = {
    import schema.{egraphs, orders, products, celebrities}
    from(egraphs, orders, products, celebrities)((egraph, order, product, celebrity) =>
      where(
        order.recipientId === customer.id and
          egraph.orderId === order.id and
          egraph._egraphState === EgraphState.Published.name and
          order.productId === product.id and
          product.celebrityId === celebrity.id
      )
        select(FulfilledOrderBundle(egraph, order, product, celebrity))
        orderBy (egraph.id desc)
    ).page(0, Utils.defaultPageLength)
  }

  /**
   * These are 5 "good" egraphs and 5 "bad" ones based on audio.
   * This is only used by the consumer-app exploration.
   */
  def findSpecificFulfilled(orderIds: List[Int]): Iterable[FulfilledOrderBundle] = {
    import schema.{egraphs, orders, products, celebrities}
    from(egraphs, orders, products, celebrities)((egraph, order, product, celebrity) =>
      where(
        order.id in orderIds and
          egraph.orderId === order.id and
          egraph._egraphState === EgraphState.Published.name and
          order.productId === product.id and
          product.celebrityId === celebrity.id
      )
        select(FulfilledOrderBundle(egraph, order, product, celebrity))
    ).page(0, Utils.defaultPageLength)
  }

  /**
   * Returns the most recently fulfilled egraphs created by a [[models.Celebrity]]
   *
   * @param celebrityId ID of the celebrity.
   */
  def findMostRecentlyFulfilledByCelebrity(celebrityId: Long): Iterable[FulfilledProductOrder] = {
    import schema.{celebrities, products, orders, egraphs}

    from(celebrities, products, orders, egraphs)((celeb, product, order, egraph) =>
      where(
        celeb.id === celebrityId and
        celeb.id === product.celebrityId and
        product.id === order.productId and
        order.id === egraph.orderId and
        egraph._egraphState === EgraphState.Published.name and
        order._privacyStatus === PrivacyStatus.Public.name
      )
      select(FulfilledProductOrder(product, order, egraph))
      orderBy (egraph.created desc)
    )
  }

  /**
   * Finds a list of Orders based on the id of the Celebrity
   * who owns the Product that was purchased.
   */
  def findByCelebrity(celebrityId: Long, filters: FilterOneTable[Order]*): Query[Order] = {
    import schema.{celebrities, products, orders}

    from(celebrities, products, orders)((celebrity, product, order) =>
      where(
        celebrity.id === celebrityId and
          celebrity.id === product.celebrityId and
          product.id === order.productId and
          FilterOneTable.reduceFilters(filters, order)
      )
        select (order)
        orderBy (order.id asc)
    )
  }

  /**
   * Retrieves the list of Orders and Egraphs that are candidates for presentation on the
   * user's gallery page. These are any orders that have not been rejected by an admin
   * and their adjoining egraphs (regardless of the adjoining egraph state)
   */
  def galleryOrdersWithEgraphs(recipientId: Long) : Query[(Order, Option[Egraph])] = {
    join(schema.orders, schema.egraphs.leftOuter) (
      (order, egraph) =>
        where(
          (order.recipientId === recipientId)
          and not (order._reviewStatus === OrderReviewStatus.RejectedByAdmin.name)
        )
        select(order, egraph)
        on(order.id === egraph.map(_.orderId))
    )
  }
  
  /**
   * Retrieves the list of Orders and Egraphs that are candidates for presentation on the
   * user's gallery page AS GIFTS. These are any orders that have not been rejected by an admin
   * and their adjoining egraphs (regardless of the adjoining egraph state)
   */
  def galleryGiftsGivenOrdersWithEgraphs(buyerId: Long) : Query[(Order, Option[Egraph])] = {
    join(schema.orders, schema.egraphs.leftOuter) (
      (order, egraph) =>
        where(
          (order.buyerId === buyerId)
          and not (order.recipientId === buyerId)
          and not (order._reviewStatus === OrderReviewStatus.RejectedByAdmin.name)
        )
        select(order, egraph)
        on(order.id === egraph.map(_.orderId))
    )
  }

  def findByBuyerCustomerId(customerId: Long, filters: FilterOneTable[Order]*): Query[Order] = {
    import schema.orders

    from(orders)(order =>
      where(
          order.buyerId === customerId and
          FilterOneTable.reduceFilters(filters, order)
      )
        select (order)
        orderBy (order.id asc)
    )
  }
  
  def findByRecipientCustomerId(customerId: Long, filters: FilterOneTable[Order]*): Query[Order] = {
    import schema.orders

    from(orders)(order =>
      where(
          order.recipientId === customerId and
          FilterOneTable.reduceFilters(filters, order)
      )
        select (order)
        orderBy (order.id asc)
    )
  }

  def getOrderResults(filters: FilterOneTable[Order]*): Query[(Order, Celebrity)] = {
    join(schema.orders, schema.products, schema.celebrities)(
      (order, product, celebrity) =>
        where(FilterOneTable.reduceFilters(filters, order))
          select(order, celebrity)
          orderBy (order.id desc)
          on(order.productId === product.id, product.celebrityId === celebrity.id)
    )
  }

  def getCelebrityOrderResults(celebrity: Celebrity, filters: FilterOneTable[Order]*): Query[(Order, Celebrity)] = {
    val celebrityId = celebrity.id
    join(schema.orders, schema.products, schema.celebrities)(
      (order, product, celebrity) =>
        where(FilterOneTable.reduceFilters(filters, order) and celebrity.id === celebrityId)
          select(order, celebrity)
          orderBy (order.id desc)
          on(order.productId === product.id, product.celebrityId === celebrity.id)
    )
  }

  /**
   * To calculate the remaining inventory available in an InventoryBatch, the number of Orders that have been placed
   * against that InventoryBatch must be known. This method returns the total number of Orders placed againsts all
   * InventoryBatches of interest.
   *
   * @return the total number of Orders placed againsts all InventoryBatches denoted by inventoryBatchIds
   */
  def countOrders(inventoryBatchIds: Seq[Long]): Int = {
    from(schema.orders)(order =>
      where(order.inventoryBatchId in inventoryBatchIds)
        compute (count)
    ).toInt
  }

  /**
   * To calculate the remaining inventory available in an InventoryBatch, the number of Orders that have been placed
   * against that InventoryBatch must be known. This method returns the total number of Orders placed against each
   * InventoryBatch of interest.
   *
   * @return tuples of (inventoryBatchId, orderCount)
   */
  def countOrdersByInventoryBatch(inventoryBatchIds: Seq[Long]): Seq[(Long, Int)] = {
    import org.squeryl.dsl.GroupWithMeasures
    val query: Query[GroupWithMeasures[LongType, LongType]] = from(schema.orders)(order =>
      where(order.inventoryBatchId in inventoryBatchIds)
        groupBy (order.inventoryBatchId)
        compute (count)
    )
    query.toSeq.map(f => (f.key, f.measures.toInt))
  }

  //
  // SavesWithLongKey[Order] methods
  //
  override val table = schema.orders


  //
  // SavesCreatedUpdated[Order] methods
  //
  override def withCreatedUpdated(toUpdate: Order, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

class OrderQueryFilters @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  def actionableOnly = List(actionableEgraphs, approvedByAdmin)

  private def actionableEgraphs: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        val nonActionableStates = Seq(EgraphState.Published.name, EgraphState.ApprovedByAdmin.name,
          EgraphState.AwaitingVerification.name, EgraphState.PassedBiometrics.name, EgraphState.FailedBiometrics.name)

        notExists(
          from(schema.egraphs)(egraph =>
            where((egraph.orderId === order.id) and
              (egraph._egraphState in nonActionableStates))
              select(egraph.id)
          )
        )
      }
    }
  }

  private def approvedByAdmin: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order._reviewStatus === OrderReviewStatus.ApprovedByAdmin.name)
      }
    }
  }

  def pendingAdminReview: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order._reviewStatus === OrderReviewStatus.PendingAdminReview.name)
      }
    }
  }

  def rejectedByAdmin: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order._reviewStatus === OrderReviewStatus.RejectedByAdmin.name)
      }
    }
  }

  def rejectedByCelebrity: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order._reviewStatus === OrderReviewStatus.RejectedByCelebrity.name)
      }
    }
  }

  def orderId(id: Long): FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order.id === id)
      }
    }
  }
}

/**
 * Factory object for creating ViewModels from Orders and Option(Egraphs)
 */

object GalleryOrderFactory {

  protected val dateFormat = new SimpleDateFormat("MMM dd, yyyy K:mma z")

  /**
   * Helper function for filtering out rejected egraphs to create a list of pending orders and optional
   * associated egraphs.
   * 
   * @param ordersAndEgraphs list of orders and associated egraphs
   * @return list of orders and egraphs that can be considered pending to a user. There can be more than one
   * egraph associated with a particular order.
   */
  def filterPendingOrders(ordersAndEgraphs: Iterable[(Order, Option[Egraph])]): Iterable[(Order, Option[Egraph])] =
  {
    ordersAndEgraphs.filter(orderEgraph => {
      val maybeEgraph = orderEgraph._2
      maybeEgraph match {
        // Order-egraph combos without Egraphs are pending
        case None => true
        case Some(egraph) => egraph.isPendingEgraph
      }
    })
  }

  def makeFulfilledEgraphViewModel[A](
      orders: Iterable[(Order, Egraph)], 
      fbAppId: String,
      consumerApp: ConsumerApplication
      )(implicit request: RequestHeader): Iterable[FulfilledEgraphViewModel] = 
  {
    for ((order, egraph) <- orders) yield {
      val product = order.product
      val celebrity = product.celebrity

      // TODO SER-170 this code is quite similar to that in GetEgraphEndpoint.
      // Refactor together and put withSigningOriginOffset inside EgraphImage.
      val rawImage = egraph.image(product.photoImage).asPng
        .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
        .scaledToWidth(product.frame.thumbnailWidthPixels)
      val thumbnailUrl = rawImage.getSavedUrl(accessPolicy = AccessPolicy.Public)
      val viewEgraphUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)

      val facebookShareLink = Facebook.getEgraphShareLink(fbAppId = fbAppId,
        fulfilledOrder = FulfilledOrder(order = order, egraph = egraph),
        thumbnailUrl = thumbnailUrl,
        viewEgraphUrl = viewEgraphUrl)
      val twitterShareLink = Twitter.getEgraphShareLink(celebrity = celebrity, viewEgraphUrl = viewEgraphUrl)

      new FulfilledEgraphViewModel(
        buyerId = order.buyerId,
        facebookShareLink = facebookShareLink,
        twitterShareLink = twitterShareLink,
        orderId = order.id,
        orientation = product.frame.name.toLowerCase,
        productUrl = StorefrontChoosePhotoConsumerEndpoints.url(celebrity, product).url,
        productPublicName = product.celebrity.publicName,
        productTitle = product.storyTitle,
        productDescription = product.description,
        recipientId = order.recipientId,
        recipientName = order.recipientName,
        thumbnailUrl = thumbnailUrl,
        viewEgraphUrl = viewEgraphUrl,
        publicStatus = order.privacyStatus.name,
        signedTimestamp = dateFormat.format(egraph.created)
      )
    }
  }

  def makePendingEgraphViewModel(orders: Iterable[(Order, Option[Egraph])]) : Iterable[PendingEgraphViewModel] = {
    for (orderAndEgraphOption <- orders) yield {
      val (order, _) = orderAndEgraphOption
      val product = order.product
      val celebrity = product.celebrity
      val imageUrl = product.photo.resizedWidth(product.frame.pendingWidthPixels).getSaved(AccessPolicy.Public).url
      val egraphExplanationUrl = controllers.routes.WebsiteControllers.getEgraphExplanationCard(order.id).url
      PendingEgraphViewModel(
        buyerId = order.buyerId,
        orderId = order.id,
        orientation = product.frame.name.toLowerCase,
        productUrl = StorefrontChoosePhotoConsumerEndpoints.url(celebrity, product).url,
        productTitle = product.storyTitle,
        productPublicName = celebrity.publicName,
        productDescription = product.description,
        recipientId = order.recipientId,
        recipientName = order.recipientName,
        thumbnailUrl = imageUrl,
        egraphExplanationUrl = egraphExplanationUrl,
        orderStatus = order.reviewStatus.name,
        orderDetails = new OrderDetails(
          orderDate = dateFormat.format(order.created),
          orderNumber = order.id,
          price = order.amountPaid.toString,
          statusText = "Pending",
          shippingMethod = "",
          UPSNumber = ""
        )
      )
    }
  }
}
