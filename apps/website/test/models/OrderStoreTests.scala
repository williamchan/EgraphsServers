package models

import enums._
import utils.EgraphsUnitTest
import services.AppConfig
import utils.TestData
import utils.DBTransactionPerTest

class OrderStoreTests extends EgraphsUnitTest with DBTransactionPerTest {

  private def orderStore = AppConfig.instance[OrderStore]
  private def orderQueryFilters = AppConfig.instance[OrderQueryFilters]
  
  "findByCustomerId" should "return orders with the customer as intended recipient" in new EgraphsTestApplication {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()

    val order = recipient.buyUnsafe(TestData.newSavedProduct(), recipient=recipient).save()

    orderStore.findByRecipientCustomerId(recipient.id).size should be (1)

    val order2 = recipient.buyUnsafe(TestData.newSavedProduct(), recipient=recipient).save()

    orderStore.findByRecipientCustomerId(recipient.id).size should be (2)

    val order3 = buyer.buyUnsafe(TestData.newSavedProduct(), recipient=recipient).save()

    orderStore.findByRecipientCustomerId(recipient.id).size should be (3)

  }

  "galleryOrdersWithEgraphs" should "return orders and their associated egraphs" in new EgraphsTestApplication {
    val (buyer, recipient, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()
    celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()

    val order1 = buyer.buyUnsafe(product, recipient=recipient).save()
    val order2 = recipient.buyUnsafe(product, recipient=recipient).save()

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
      buyer.buyUnsafe(product, recipient=recipient).withReviewStatus(status).save()
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
    customer.buyUnsafe(product1).save()
    customer.buyUnsafe(product2).save()
    customer.buyUnsafe(product2).save()

    val inventoryBatchIds = Seq(inventoryBatch1.id, inventoryBatch2.id)
    orderStore.countOrders(inventoryBatchIds) should be(3)
  }

  "findByFilter" should "restrict by filter but not by celebrity" in new EgraphsTestApplication {
    //This test will not be able to be run in parallel with other tests as written.
    val numfound = orderStore.getOrderResults().toSeq.length

    val (customer0, product0) = newCustomerAndProduct
    val order0 = customer0.buyUnsafe(product0).save()
    val (customer1, product1) = newCustomerAndProduct
    val order1 = customer1.buyUnsafe(product1).save()

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
    customer.buyUnsafe(product1).save()
    customer.buyUnsafe(product2).save()
    customer.buyUnsafe(product3).save()

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
      customer.buyUnsafe(product).save(),
      customer.buyUnsafe(product).save(),
      customer.buyUnsafe(product).save()
    )

    // Orders of celebrity's products
    val allCelebOrders = orderStore.findByCelebrity(celebrity.id)
    allCelebOrders.toSeq should have length (3)
    allCelebOrders.toSet should be (Set(firstOrder, secondOrder, thirdOrder))
  }

  it should "not find any other Celebrity's orders" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    val (_, _ , _, otherCelebrityProduct) = TestData.newSavedOrderStack()

    val celebOrder = will.buyUnsafe(product).save()
    will.buyUnsafe(otherCelebrityProduct).save()

    val celebOrders = orderStore.findByCelebrity(celebrity.id)

    celebOrders.toSeq should have length(1)
    celebOrders.head should be (celebOrder)
  }

  it should "only find a particular Order when composed with OrderIdFilter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()

    val firstOrder = will.buyUnsafe(product).save()
    will.buyUnsafe(product).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.orderId(firstOrder.id))

    found.toSeq.length should be (1)
    found.head should be (firstOrder)
  }

  it should "exclude orders that have reviewStatus of PendingAdminReview, RejectedByAdmin, or RejectedByCelebrity when composed with ActionableFilter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    val actionableOrder = will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

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
        val order = will.buyUnsafe(product).approveByAdmin(admin).save()
        order.newEgraph.withEgraphState(state).save()
        (state, order)
    }

    // Also buy one without an Egraph
    val orderWithoutEgraph = will.buyUnsafe(product).approveByAdmin(admin).save()

    // Perform the test
    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*).toSeq

    found.length should be (3)
    found.contains(orderWithoutEgraph) should be(true)
    val orderWithRejectedByAdminEgraph = ordersByEgraphState.find(_._1 == EgraphState.RejectedByAdmin).get._2
    found.contains(orderWithRejectedByAdminEgraph) should be(true)
    val orderWithRejectedByMlbEgraph = ordersByEgraphState.find(_._1 == EgraphState.RejectedByMlb).get._2
    found.contains(orderWithRejectedByMlbEgraph) should be(true)
  }

  it should "only include orders that are pendingAdminReview when composed with that filter" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    val pendingOrder = will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.pendingAdminReview)
    found.toSeq.length should be(1)
    found.toSet should be(Set(pendingOrder))
  }

  it should "only include orders that are rejected when composed with those filters" in new EgraphsTestApplication {
    val (will, _, celebrity, product) = TestData.newSavedOrderStack()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    val orderRejectedByAdmin = will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    val orderRejectedByCelebrity = will.buyUnsafe(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    orderStore.findByCelebrity(celebrity.id, orderQueryFilters.rejectedByAdmin).head should be(orderRejectedByAdmin)
    orderStore.findByCelebrity(celebrity.id, orderQueryFilters.rejectedByCelebrity).head should be(orderRejectedByCelebrity)
  }

  private def newCustomerAndProduct: (Customer, Product) = {
    (TestData.newSavedCustomer(), TestData.newSavedProduct())
  }
}
