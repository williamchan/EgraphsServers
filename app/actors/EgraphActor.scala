package actors

import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import models.EgraphStore
import com.google.inject.Inject
import services.logging.{Logging, LoggingContext}
import models.enums.EgraphState
import services.http.PlayConfig
import java.util.Properties

/**
 * Actor that performs most of the work of creating an egraph and running it through our biometrics
 * tests.
 *
 * @param db for access to a database connection
 * @param egraphStore a store for accessing egraphs from persistence
 * @param logging for generating useful logs.
 */
case class EgraphActor @Inject() (
  db: DBSession,
  egraphStore: EgraphStore,
  logging: LoggingContext,
  @PlayConfig playConfig: Properties
) extends Actor with Logging
{
  protected def receive = {
    case ProcessEgraphMessage(id: Long) => {
      processEgraph(egraphId = id)
    }

    case _ =>
  }

  private def processEgraph(egraphId: Long) {
    playConfig.getProperty("biometrics.status") match {
      case "offline" =>
      case _ => {
        logging.withTraceableContext("processEgraph[" + egraphId + "]") {
          db.connected(TransactionSerializable) {
            egraphStore.get(egraphId) match {
              case egraph if (egraph.egraphState == EgraphState.AwaitingVerification) => {
                val testedEgraph = egraph.verifyBiometrics.save()

                // If admin review is turned off (eg to expedite demos), immediately publish regardless of biometric results
                if (playConfig.getProperty("adminreview.skip") == "true") {
                  val publishedEgraph = testedEgraph.withEgraphState(EgraphState.Published).save()
                  publishedEgraph.order.sendEgraphSignedMail()
                }
              }
            }
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
case class ProcessEgraphMessage(id: Long) extends EgraphMessage
