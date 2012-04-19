package actors

import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import models.Egraph.EgraphState
import models.EgraphStore
import com.google.inject.Inject
import services.logging.{Logging, LoggingContext}

object EgraphActor {
  val actor = actorOf(AppConfig.instance[EgraphActor])
}

class EgraphActor @Inject() (
  db: DBSession,
  egraphStore: EgraphStore,
  logging: LoggingContext
) extends Actor with Logging
{
  protected def receive = {
    case ProcessEgraphMessage(id: Long, skipBiometrics: Boolean) => {
      processEgraph(egraphId = id, skipBiometrics = skipBiometrics)
    }

    case _ =>
  }

  def processEgraph(egraphId: Long, skipBiometrics: Boolean) {
    logging.withTraceableContext("processEgraph[" +egraphId +"," + skipBiometrics +"]") {
      log("Processing Egraph " + egraphId)
      db.connected(TransactionSerializable) {
        val egraph = egraphStore.findById(egraphId)
        if (egraph.isEmpty) {
          throw new Exception("EgraphActor could not find Egraph " + egraphId.toString)
        }

        if (egraph.get.stateValue == EgraphState.AwaitingVerification.value) {
          egraph.get.assets.initMasterImage()
          val egraphToTest = if (skipBiometrics) egraph.get.withYesMaamBiometricServices else egraph.get
          val testedEgraph = egraphToTest.verifyBiometrics.save()
          if (testedEgraph.state == EgraphState.Published) {
            testedEgraph.order.sendEgraphSignedMail()
          }
        }
      }
    }
  }
}

// ====================
// ===== Messages =====
// ====================
sealed trait EgraphMessage
case class ProcessEgraphMessage(id: Long, skipBiometrics: Boolean = false) extends EgraphMessage
