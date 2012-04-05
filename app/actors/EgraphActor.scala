package actors

import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import models.{EgraphStore, EgraphState}

object EgraphActor {

  val actor = actorOf(new EgraphActor)

  private val db = AppConfig.instance[DBSession]
  private val egraphStore = AppConfig.instance[EgraphStore]

  def processEgraph(egraphId: Long, skipBiometrics: Boolean) {
    play.Logger.info("EgraphActor: Processing Egraph " + egraphId)
    db.connected(TransactionSerializable) {
      val egraph = egraphStore.findById(egraphId)
      if (egraph.isEmpty) {
        throw new Exception("EgraphActor could not find Egraph " + egraphId.toString)
      }

      if (egraph.get.stateValue == EgraphState.AwaitingVerification.value) {
        egraph.get.assets.initMasterImage()
        val egraphToTest = if (skipBiometrics) egraph.get.withYesMaamBiometricServices else egraph.get
        val testedEgraph = egraphToTest.verifyBiometrics.save()
        if (testedEgraph.state == EgraphState.Verified) {
          testedEgraph.order.sendEgraphSignedMail()
        }
      }
    }
  }
}

class EgraphActor extends Actor {

  protected def receive = {
    case ProcessEgraphMessage(id: Long, skipBiometrics: Boolean) => {
      EgraphActor.processEgraph(egraphId = id, skipBiometrics = skipBiometrics)
    }
    case _ =>
  }


}

// ====================
// ===== Messages =====
// ====================
sealed trait EgraphMessage
case class ProcessEgraphMessage(id: Long, skipBiometrics: Boolean = false) extends EgraphMessage
