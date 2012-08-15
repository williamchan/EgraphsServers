package services.actors

import actors.{ProcessEgraphMessage, EgraphActor}
import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.{Utils, AppConfig}
import models.EgraphStore
import akka.util.TestKit
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest, TestData}
import models.enums.EgraphState
import org.scalatest.BeforeAndAfterAll

class EgraphActorTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with BeforeAndAfterAll
  with TestKit {

  private val egraphActor = actorOf(AppConfig.instance[EgraphActor])
  private val db = AppConfig.instance[DBSession]
  private val egraphStore = AppConfig.instance[EgraphStore]

  override protected def beforeAll() {
    egraphActor.start()
  }

  override protected def afterAll() {
    egraphActor.stop()
  }

  it should "respond when an Egraph cannot be found by id" in {
    evaluating {
      egraphActor !! ProcessEgraphMessage(0L)
    } should produce[Exception]
  }

  it should "process Egraph" in {
    val egraph1 = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }
    val egraph2 = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }
    egraph1.egraphState should be(EgraphState.AwaitingVerification)
    egraph2.egraphState should be(EgraphState.AwaitingVerification)

    egraphActor !! ProcessEgraphMessage(egraph1.id)
    egraphActor !! ProcessEgraphMessage(egraph2.id)
    db.connected(TransactionSerializable) {
      egraphStore.get(egraph1.id).egraphState should be(EgraphState.PassedBiometrics)
      egraphStore.get(egraph2.id).egraphState should be(EgraphState.PassedBiometrics)
    }
  }

  it should "initialize mp3 asset from wav asset" in  {
    import services.blobs.Blobs.Conversions._
    val egraph = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }
    egraphActor !! ProcessEgraphMessage(egraph.id)
    egraph.assets.audioMp3.asByteArray.length should be > (0)
  }

  it should "immediately publish an Egraph if play config's adminreview.skip is true" in {
    val egraph = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }

    val actor = actorOf(AppConfig.instance[EgraphActor].copy(playConfig=Utils.properties("adminreview.skip" -> "true")))
    actor.start()
    actor !! ProcessEgraphMessage(egraph.id)
    db.connected(TransactionSerializable) {
      egraphStore.get(egraph.id).egraphState should be(EgraphState.Published)
    }
  }

  it should "be automatically reject the Egraph if a non-rejected Egraph already exists for the associated Order" in {
    val egraph = db.connected(TransactionSerializable) {
      val otherEgraph = TestData.newSavedEgraphWithRealAudio().withEgraphState(EgraphState.AwaitingVerification).save()
      TestData.newSavedEgraphWithRealAudio().copy(orderId = otherEgraph.orderId).save()
    }
    egraph.egraphState should be(EgraphState.AwaitingVerification)

    egraphActor !! ProcessEgraphMessage(egraph.id)
    db.connected(TransactionSerializable) {
      egraphStore.get(egraph.id).egraphState should be(EgraphState.RejectedByAdmin)
    }
  }
}
