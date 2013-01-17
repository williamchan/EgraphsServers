package actors

import akka.actor.Actor
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import models.{EgraphQueryFilters, EgraphStore}
import com.google.inject.Inject
import services.logging.{Logging, LoggingContext}
import models.enums.EgraphState
import play.api.Play.current
import akka.actor.Props
import play.api.libs.concurrent.Akka
import services.config.ConfigFileProxy
import play.api.mvc.RequestHeader

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
  egraphQueryFilters: EgraphQueryFilters,
  logging: LoggingContext,
  config: ConfigFileProxy
) extends Actor with Logging
{
  protected def receive = {
    case ProcessEgraphMessage(egraphId, requestHeader) => {
      processEgraph(egraphId, requestHeader)
      sender ! "Success"
    }

    case _ =>
  }

  /**
   * Run biometrics on the Egraph and updates the EgraphState based on the result.
   * Also, initializes the MP3 from the WAV (Egraphs are typically created with WAVs) and stores it to the blobstore.
   *
   * If the application config paramter "adminreview.skip" is turned on, then the Egraph is immediately published.
   *
   * @param egraphId id of the Egraph to process
   */
  private def processEgraph[A](egraphId: Long, requestHeader: RequestHeader) {
    config.biometricsStatus match {
      case "offline" =>
      case _ => {
        logging.withTraceableContext("processEgraph[" + egraphId + "]") {
          db.connected(TransactionSerializable) {
            val egraph = egraphStore.get(egraphId)

            // Check that there is not another Egraph out there. This helps guard against accidentally approving or
            // publishing multiple Egraphs for the same Order.
            if (egraphStore.findByOrder(egraph.orderId, egraphQueryFilters.notRejected).size > 1) {
              egraph.withEgraphState(EgraphState.RejectedByAdmin).save()
            }

            else if (egraph.egraphState == EgraphState.AwaitingVerification) {
              val testedEgraph = egraph.verifyBiometrics.save()

              egraph.assets.generateAndSaveMp3()
              egraph.assets.generateAndSaveMp4()

              // If admin review is turned off (eg to expedite demos), immediately publish regardless of biometric results
              if (config.adminreviewSkip) {
                val publishedEgraph = testedEgraph.withEgraphState(EgraphState.Published).save()
                publishedEgraph.order.sendEgraphSignedMail(requestHeader)
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
  val actor = Akka.system.actorOf(Props(AppConfig.instance[EgraphActor]))
}

// ====================
// ===== Messages =====
// ====================
sealed trait EgraphMessage

case class ProcessEgraphMessage[A](egraphId: Long, requestHeader: RequestHeader) extends EgraphMessage
