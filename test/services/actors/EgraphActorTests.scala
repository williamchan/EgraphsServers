package services.actors

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import actors.{ProcessEgraphMessage, EgraphActor}
import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import models.{Egraph, EgraphStore, EgraphState, EgraphTests}
import utils.{ClearsDatabaseAndValidationAfter, TestConstants}
import akka.util.TestKit
import org.scalatest.BeforeAndAfterAll

class EgraphActorTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterAll
with ClearsDatabaseAndValidationAfter
with TestKit {

  private val egraphActor = actorOf(AppConfig.instance[EgraphActor])

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
    var egraph1: Egraph = null
    var egraph2: Egraph = null
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      egraph1 = EgraphTests.persistedOrder
        .newEgraph
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.voiceStr().getBytes("UTF-8"))
        .save()
    }
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      egraph2 = EgraphTests.persistedOrder
        .newEgraph
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.voiceStr().getBytes("UTF-8"))
        .save()
    }
    egraph1.stateValue should be(EgraphState.AwaitingVerification.value)
    egraph2.stateValue should be(EgraphState.AwaitingVerification.value)

    egraphActor !! ProcessEgraphMessage(egraph1.id)
    egraphActor !! ProcessEgraphMessage(egraph2.id)
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      val egraphStore = AppConfig.instance[EgraphStore]
      egraphStore.findById(egraph1.id).get.stateValue should be(EgraphState.Verified.value)
      egraphStore.findById(egraph2.id).get.stateValue should be(EgraphState.Verified.value)
    }
  }

}
