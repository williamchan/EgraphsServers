package actors

import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import models.Egraph.EgraphState
import models.EgraphStore
import com.google.inject.Inject
import services.logging.{Logging, LoggingContext}

/**
 * Actor that performs most of the work of creating an egraph and running it through our biometrics
 * tests.
 *
 * @param db for access to a database connection
 * @param egraphStore a store for accessing egraphs from persistence
 * @param logging for generating useful logs.
 */
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
      db.connected(TransactionSerializable) {
        val egraph = egraphStore.findById(egraphId)
        if (egraph.isEmpty) {
          throw new Exception("EgraphActor could not find Egraph " + egraphId.toString)
        }

        if (egraph.get.stateValue == EgraphState.AwaitingVerification.value) {
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


object EgraphActor {
  // Singleton instance of EgraphActor allows only one egraph to be fulfilled at a time.
  // TODO: remove this singleton instance, make all access to this actor go through actorOf.
  val actor = actorOf(AppConfig.instance[EgraphActor])
}

// ====================
// ===== Messages =====
// ====================
sealed trait EgraphMessage
case class ProcessEgraphMessage(id: Long, skipBiometrics: Boolean = false) extends EgraphMessage
