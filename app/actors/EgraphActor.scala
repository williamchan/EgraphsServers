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
      val savedEgraph = egraphStore.findById(egraphId)
      if (savedEgraph.isEmpty) {
        throw new Exception("EgraphActor could not find Egraph " + egraphId.toString)
      }

      savedEgraph.get.assets.initMasterImage()
      val egraphToTest = if (skipBiometrics) savedEgraph.get.withYesMaamBiometricServices else savedEgraph.get
      val testedEgraph = egraphToTest.verifyBiometrics.save()
      if (testedEgraph.state == EgraphState.Verified) {
        testedEgraph.order.sendEgraphSignedMail()
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
