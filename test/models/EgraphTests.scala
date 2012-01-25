package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class EgraphTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Egraph]
  with CreatedUpdatedEntityTests[Egraph]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val store = AppConfig.instance[EgraphStore]

  //
  // SavingEntityTests[Egraph] methods
  //
  override def newEntity = {
    val order = persistedOrder
    Egraph(orderId=order.id)
  }

  override def saveEntity(toSave: Egraph) = {
    toSave.saveWithoutAssets()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Egraph) = {
    val order = persistedOrder
    toTransform.copy(
      orderId = order.id,
      stateValue = Verified.value
    )
  }

  //
  // Test cases
  //
  "An Egraph" should "update its state when withState is called" in {
    val egraph = Egraph().withState(RejectedVoice)

    egraph.state should be (RejectedVoice)
  }

  it should "save and recover signature and audio data from the blobstore" in {
    val egraph = persistedOrder
      .newEgraph
      .save(TestConstants.signatureStr, "my audio".getBytes("UTF-8"))

    egraph.assets.signature should be (TestConstants.signatureStr)
    egraph.assets.audio.toArray should be ("my audio".getBytes("UTF-8"))
  }

  it should "throw an exception if assets are accessed on an unsaved Egraph" in {
    evaluating { persistedOrder.newEgraph.assets } should produce [IllegalArgumentException]
  }

  "Egraph statuses" should "be accessible via their values on the companion object" in {
    Egraph.states.foreach( stateTuple =>
      Egraph.states(stateTuple._2.value) should be (stateTuple._2)
    )
  }

  "allStatuses" should "contain all the states" in {
    Egraph.states.size should be (6)
  }

  it should "throw an exception at an unrecognized string" in {
    evaluating { Egraph.states("Herpyderp") } should produce [NoSuchElementException]
  }

  //
  // Private methods
  //
  def persistedOrder: Order = {
    val customer = TestData.newSavedCustomer()
    val celebrity = Celebrity().save()
    val product = celebrity.newProduct.save()

    customer.buy(product).save()
  }

}