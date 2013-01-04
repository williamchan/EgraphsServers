package models

import enums._
import utils.EgraphsUnitTest
import services.db.DBSession
import services.AppConfig
import utils.TestData
import utils.DBTransactionPerTest

class OrderStoreTests extends EgraphsUnitTest with DBTransactionPerTest {
  private def db = AppConfig.instance[DBSession]
  private def orderStore = AppConfig.instance[OrderStore]
  private def orderQueryFilters = AppConfig.instance[OrderQueryFilters]
  
  "findByCustomerId" should "return orders with the customer as intended recipient" in new EgraphsTestApplication {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()

    val order = recipient.buy(TestData.newSavedProduct(), recipient=recipient).save()

    orderStore.findByRecipientCustomerId(recipient.id).size should be (1)

    val order2 = recipient.buy(TestData.newSavedProduct(), recipient=recipient).save()

    orderStore.findByRecipientCustomerId(recipient.id).size should be (2)

    val order3 = buyer.buy(TestData.newSavedProduct(), recipient=recipient).save()

    orderStore.findByRecipientCustomerId(recipient.id).size should be (3)

  }

  "galleryOrdersWithEgraphs" should "return orders and their associated egraphs" in new EgraphsTestApplication {
    val (buyer, recipient, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()
    celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()

    val order1 = buyer.buy(product, recipient=recipient).save()
    val order2 = recipient.buy(product, recipient=recipient).save()

    val egraph = order1.newEgraph.save()
    egraph.verifyBiometrics.approve(admin).publish(admin).save()

    val results = orderStore.galleryOrdersWithEgraphs(recipient.id)

    results.size should be (2)

    val queriedEgraph = results.head._2.get

    queriedEgraph.id should be (egraph.id)
  }
  
  it should "exclude orders that have been rejected by an admin" in new EgraphsTestApplication {
    import OrderReviewStatus._
    
    val (buyer, recipient, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()
    celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
    
    def buyAndSetReviewStatus(status: OrderReviewStatus.EnumVal):Order = {
      buyer.buy(product, recipient=recipient).withReviewStatus(status).save()
    }
    
    for (status <- OrderReviewStatus.values) buyAndSetReviewStatus(status)
    
    val queriedStati = orderStore.galleryOrdersWithEgraphs(recipient.id).map { orderAndEgraph => 
      orderAndEgraph._1.reviewStatus
    }

    queriedStati.toSet should be (Set(ApprovedByAdmin, PendingAdminReview, RejectedByCelebrity))
  }
  
  "countOrders" should "return count of orders made against InventoryBatches" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val product1 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product2 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val inventoryBatch1 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val inventoryBatch2 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    inventoryBatch1.products.associate(product1)
    inventoryBatch2.products.associate(product2)
    customer.buy(product1).save()
    customer.buy(product2).save()
    customer.buy(product2).save()

    val inventoryBatchIds = Seq(inventoryBatch1.id, inventoryBatch2.id)
    orderStore.countOrders(inventoryBatchIds) should be(3)
  }

  "findByFilter" should "restrict by filter but not by celebrity" in new EgraphsTestApplication {
    //This test will not be able to be run in parallel with other tests as written.
    val numfound = orderStore.getOrderResults().toSeq.length

    val (customer0, product0) = newCustomerAndProduct
    val order0 = customer0.buy(product0).save()
    val (customer1, product1) = newCustomerAndProduct
    val order1 = customer1.buy(product1).save()

    val foundAfter = orderStore.getOrderResults().toSeq.length
    val newOrders = (foundAfter - numfound)
    newOrders should be(2)
  }

  "countOrdersByInventoryBatch" should "return tuples of inventoryBatch's id and orders placed against that batch" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val product1 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product2 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product3 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val inventoryBatch1 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val inventoryBatch2 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val inventoryBatch3 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    inventoryBatch1.products.associate(product1)
    inventoryBatch1.products.associate(product2)
    inventoryBatch2.products.associate(product3)
    customer.buy(product1).save()
    customer.buy(product2).save()
    customer.buy(product3).save()

    val inventoryBatchIds = Seq(inventoryBatch1.id, inventoryBatch2.id, inventoryBatch3.id)
    val inventoryBatchIdsAndOrderCount = orderStore.countOrdersByInventoryBatch(inventoryBatchIds)
    inventoryBatchIdsAndOrderCount.length should be(2) // Query excludes rows with zero count
    val resultForInventoryBatch1 = inventoryBatchIdsAndOrderCount.find(p => p._1 == inventoryBatch1.id)
    resultForInventoryBatch1.isDefined should be(true)
    resultForInventoryBatch1.get._2 should be(2)
    val resultForInventoryBatch2 = inventoryBatchIdsAndOrderCount.find(p => p._1 == inventoryBatch2.id)
    resultForInventoryBatch2.isDefined should be(true)
    resultForInventoryBatch2.get._2 should be(1)
  }
  
  "findByCelebrity" should "find all of a Celebrity's orders by default" in new EgraphsTestApplication {

    val (customer, _, celebrity, product) = TestData.newSavedOrderStack()

    val (firstOrder, secondOrder, thirdOrder) = (
      customer.buy(product).save(),
      customer.buy(product).save(),
      customer.buy(product).save()
    )

    // Orders of celebrity's products
    val allCelebOrders = orderStore.findByCelebrity(celebrity.id)
    allCelebOrders.toSeq should have length (3)
    allCelebOrders.toSet should be (Set(firstOrder, secondOrder, thirdOrder))
  }

  it should "not find any other Celebrity's orders" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    val (_, _ , _, otherCelebrityProduct) = TestData.newSavedOrderStack()

    val celebOrder = will.buy(product).save()
    will.buy(otherCelebrityProduct).save()

    val celebOrders = orderStore.findByCelebrity(celebrity.id)

    celebOrders.toSeq should have length(1)
    celebOrders.head should be (celebOrder)
  }

  it should "only find a particular Order when composed with OrderIdFilter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()

    val firstOrder = will.buy(product).save()
    will.buy(product).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.orderId(firstOrder.id))

    found.toSeq.length should be (1)
    found.head should be (firstOrder)
  }

  it should "exclude orders that have reviewStatus of PendingAdminReview, RejectedByAdmin, or RejectedByCelebrity when composed with ActionableFilter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    val actionableOrder = will.buy(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*)
    found.toSeq.length should be(1)
    found.toSet should be(Set(actionableOrder))
  }

  it should "exclude orders that have Published or reviewable Egraphs when composed with ActionableFilter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()

    // Make an order for each Egraph State, and save an Egraph in that state
    val ordersByEgraphState = EgraphState.values.map {
      state =>
        val order = will.buy(product).approveByAdmin(admin).save()
        order.newEgraph.withEgraphState(state).save()
        (state, order)
    }

    // Also buy one without an Egraph
    val orderWithoutEgraph = will.buy(product).approveByAdmin(admin).save()

    // Perform the test
    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*)

    found.toSeq.length should be (2)
    val rejectedByAdminOrder = ordersByEgraphState.find(_._1 == EgraphState.RejectedByAdmin).get._2
    found.toSet should be (Set(
      orderWithoutEgraph,
      rejectedByAdminOrder
    ))
  }

  it should "only include orders that are pendingAdminReview when composed with that filter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    will.buy(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    val pendingOrder = will.buy(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.pendingAdminReview)
    found.toSeq.length should be(1)
    found.toSet should be(Set(pendingOrder))
  }

  it should "only include orders that are rejected when composed with those filters" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    will.buy(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    val orderRejectedByAdmin = will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    val orderRejectedByCelebrity = will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    orderStore.findByCelebrity(celebrity.id, orderQueryFilters.rejectedByAdmin).head should be(orderRejectedByAdmin)
    orderStore.findByCelebrity(celebrity.id, orderQueryFilters.rejectedByCelebrity).head should be(orderRejectedByCelebrity)
  }

  private def newCustomerAndProduct: (Customer, Product) = {
    (TestData.newSavedCustomer(), TestData.newSavedProduct())
  }
}
