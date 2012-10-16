package services.actors

import actors.{ProcessEgraphMessage, EgraphActor}
import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.{Utils, AppConfig}
import models.EgraphStore
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest, TestData}
import models.enums.EgraphState
import org.scalatest.BeforeAndAfterAll
import play.api.Application
import utils.TestHelpers.withActorUnderTest
import akka.pattern._
import akka.util.Timeout
import akka.util.duration._
import play.api.test.FakeRequest
import services.config.ConfigFileProxy
import akka.dispatch.Await
import models.EgraphQueryFilters
import services.logging.LoggingContext
import models.EgraphStore

class EgraphActorTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with BeforeAndAfterAll {

  implicit val timeout: Timeout = 10 second

  private def db = AppConfig.instance[DBSession]
  private def egraphStore = AppConfig.instance[EgraphStore]

  it should "process Egraph" in new EgraphsTestApplication {
    withActorUnderTest(AppConfig.instance[EgraphActor]) { egraphActor =>
      val egraph1 = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }
      val egraph2 = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }
      egraph1.egraphState should be(EgraphState.AwaitingVerification)
      egraph2.egraphState should be(EgraphState.AwaitingVerification)

      Await.result(egraphActor ask ProcessEgraphMessage(egraph1.id, FakeRequest()), 10 second)
      Await.result(egraphActor ask ProcessEgraphMessage(egraph2.id, FakeRequest()), 10 second)

      db.connected(TransactionSerializable) {
        egraphStore.get(egraph1.id).egraphState should be(EgraphState.PassedBiometrics)
        egraphStore.get(egraph2.id).egraphState should be(EgraphState.PassedBiometrics)
      }
    }
  }

  it should "initialize mp3 asset from wav asset" in new EgraphsTestApplication {
    withActorUnderTest(AppConfig.instance[EgraphActor]) { egraphActor =>
      import services.blobs.Blobs.Conversions._
      val egraph = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }

      Await.result(egraphActor ask ProcessEgraphMessage(egraph.id, FakeRequest()), 10 second)
      
      egraph.assets.audioMp3.asByteArray.length should be > (0)
    }
  }

  it should "immediately publish an Egraph if play config's adminreview.skip is true" in new EgraphsTestApplication {
    val mockConfig: ConfigFileProxy = spy(AppConfig.instance[ConfigFileProxy])
    mockConfig.adminreviewSkip returns true
    val db = AppConfig.instance[DBSession]
    val egraphStore = AppConfig.instance[EgraphStore]
    val egraphQueryFilters = AppConfig.instance[EgraphQueryFilters]
    val logging = AppConfig.instance[LoggingContext]

    withActorUnderTest(new EgraphActor(db, egraphStore, egraphQueryFilters, logging, mockConfig)) { egraphActor =>
      val egraph = db.connected(TransactionSerializable) { TestData.newSavedEgraphWithRealAudio() }

      Await.result(egraphActor ask ProcessEgraphMessage(egraph.id, FakeRequest()), 10 second)
      db.connected(TransactionSerializable) {
        egraphStore.get(egraph.id).egraphState should be(EgraphState.Published)
      }
    }
  }

  it should "be automatically reject the Egraph if a non-rejected Egraph already exists for the associated Order" in new EgraphsTestApplication {
    withActorUnderTest(AppConfig.instance[EgraphActor]) { egraphActor =>
      val egraph = db.connected(TransactionSerializable) {
        val otherEgraph = TestData.newSavedEgraphWithRealAudio().withEgraphState(EgraphState.AwaitingVerification).save()
        TestData.newSavedEgraphWithRealAudio().copy(orderId = otherEgraph.orderId).save()
      }
      egraph.egraphState should be(EgraphState.AwaitingVerification)
  
      Await.result(egraphActor ask ProcessEgraphMessage(egraph.id, FakeRequest()), 1 second)
      db.connected(TransactionSerializable) {
        egraphStore.get(egraph.id).egraphState should be(EgraphState.RejectedByAdmin)
      }
    }
  }
}
