package models

import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import libs.{Utils, Time}

abstract sealed class EgraphState(val value: String)

case object AwaitingVerification extends EgraphState("AwaitingVerification")
case object Verified extends EgraphState("Verified")
case object RejectedVocals extends EgraphState("Rejected:Vocals")
case object RejectedHandwriting extends EgraphState("Rejected:Handwriting")
case object RejectedPersonalAudit extends EgraphState("Rejected:Audit")

/**
 * Persistent entity representing a single eGraph.
 *
 * An Egraph is both the final delivered product and an attempt to fulfill an order.
 */
case class Egraph(
  id: Long = 0L,
  orderId: Long = 0L,
  signature: Array[Byte] = Array.empty,
  audio: Array[Byte] = Array.empty,
  stateValue: String = AwaitingVerification.value,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public methods
  //
  def save(): Egraph = {
    Egraph.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    // Provide a special unapplication that turns our Array[Byte]s into Seqs,
    // so equality check can be deep against the contents.
    Egraph.unapply(this).get.productIterator.map((thisField) =>
      thisField match {
        case anArray: Array[_] =>
          anArray: Seq[_]

        case _ =>
          thisField
      }
    ).toSeq
  }
}

object Egraph extends Saves[Egraph] with SavesCreatedUpdated[Egraph] {
  /** All possible states an Egraph can enter */

  def getKey(state: EgraphState): String = {
    state.value
  }

  /**
   * Map of Egraph state strings to the actual EgraphStates
   */
  val states = Utils.toMap[String, EgraphState](Seq(
    AwaitingVerification,
    Verified,
    RejectedVocals,
    RejectedHandwriting,
    RejectedPersonalAudit
  ), key=(theState) => theState.value)

  //
  // Public Methods
  //

  //
  // Saves[Egraph] methods
  //
  override val table = Schema.egraphs

  override def defineUpdate(theOld: Egraph, theNew: Egraph) = {
    import org.squeryl.PrimitiveTypeMode._

    updateIs(
      theOld.signature := theNew.signature,
      theOld.orderId := theNew.orderId,
      theOld.audio := theNew.audio,
      theOld.stateValue := theNew.stateValue,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Egraph] methods
  //
  override def withCreatedUpdated(toUpdate: Egraph, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}