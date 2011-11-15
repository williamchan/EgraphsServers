package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}

class EgraphTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Egraph]
  with CreatedUpdatedEntityTests[Egraph]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{

  //
  // SavingEntityTests[Egraph] methods
  //
  override def newEntity = {
    val order = persistedOrder
    Egraph(orderId=order.id)
  }

  override def saveEntity(toSave: Egraph) = {
    Egraph.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    Egraph.findById(id)
  }

  override def transformEntity(toTransform: Egraph) = {
    val order = persistedOrder
    toTransform.copy(
      orderId = order.id,
      signature = "herp".getBytes,
      audio = "derp".getBytes,
      stateValue = Verified.value
    )
  }

  //
  // Test cases
  //
  "An Egraph" should "update its state when withState is called" in {
    val egraph = Egraph().withState(RejectedVocals)

    egraph.state should be (RejectedVocals)
  }

  "Egraph statuses" should "be accessible via their values on the companion object" in {
    Egraph.states.foreach( stateTuple =>
      Egraph.states(stateTuple._2.value) should be (stateTuple._2)
    )
  }

  "allStatuses" should "contain all the states" in {
    Egraph.states.size should be (5)
  }

  it should "throw an exception at an unrecognized string" in {
    evaluating { Egraph.states("Herpyderp") } should produce [NoSuchElementException]
  }

  //
  // Private methods
  //
  def persistedOrder = {
    val customer = Customer().save()
    val celebrity = Celebrity().save()
    val product = celebrity.newProduct.save()

    customer.order(product).save()
  }

}