package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.blobs.Blobs.Conversions._
import services.{Time, AppConfig}

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
    val order = EgraphTests.persistedOrder
    Egraph(orderId=order.id)
  }

  override def saveEntity(toSave: Egraph) = {
    toSave.saveWithoutAssets()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Egraph) = {
    val order = EgraphTests.persistedOrder
    toTransform.copy(
      orderId = order.id,
      stateValue = EgraphState.Verified.value
    )
  }

  //
  // Test cases
  //
  "An Egraph" should "update its state when withState is called" in {
    val egraph = Egraph().withState(EgraphState.RejectedVoice)

    egraph.state should be (EgraphState.RejectedVoice)
  }

  it should "save and recover signature and audio data from the blobstore" in {
    val egraph = EgraphTests.persistedOrder
      .newEgraph
      .withAssets(TestConstants.signatureStr, Some(TestConstants.messageStr), "my audio".getBytes("UTF-8"))
      .save()

    egraph.assets.signature should be (TestConstants.signatureStr)
    egraph.assets.audio.asByteArray should be ("my audio".getBytes("UTF-8"))
  }

  it should "throw an exception if assets are accessed on an unsaved Egraph" in {
    evaluating { EgraphTests.persistedOrder.newEgraph.assets } should produce [IllegalArgumentException]
  }

  "Egraph statuses" should "be accessible via their values on the companion object" in {
    EgraphState.named.foreach( stateTuple =>
      EgraphState.named(stateTuple._2.value) should be (stateTuple._2)
    )
  }

  "allStatuses" should "contain all the states" in {
    EgraphState.named.size should be (6)
  }

  it should "throw an exception at an unrecognized string" in {
    evaluating { EgraphState.named("Herpyderp") } should produce [NoSuchElementException]
  }

  "An Egraph Story" should "render all values correctly in the title" in {
    val storyTemplate = EgraphStoryField.values.foldLeft("") { (accum, field) =>
      accum + "{" + field.name + "}"
    }
    val story = EgraphStory(
      titleTemplate = "{signer_name}",
      bodyTemplate = storyTemplate,
      celebName = "Herpy Derpson",
      celebUrlSlug = "Herpy-Derpson",
      recipientName = "Erem Recipient",
      productName = "NBA Finals 2012",
      productUrlSlug = "NBA-Finals-2012",
      orderTimestamp = Time.fromApiFormat("2012-02-10 13:00:00.256"),
      signingTimestamp = Time.fromApiFormat("2011-02-10 13:00:00.256")
    )
    story.title should be ("Herpy Derpson")
    story.body should be ("Herpy Derpson<a href='/Herpy-Derpson' >Erem RecipientNBA Finals 2012<a href='/Herpy-Derpson/NBA-Finals-2012' >February 10, 2012February 10, 2011</a>")
  }
}

object EgraphTests {

  def persistedOrder: Order = {
    val customer = TestData.newSavedCustomer()
    val celebrity = Celebrity().save()
    val product = celebrity.newProduct.save()

    customer.buy(product).save()
  }
  
}