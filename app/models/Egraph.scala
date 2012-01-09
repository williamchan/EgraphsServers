package models

import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import libs.Blobs.AccessPolicy
import javax.imageio.ImageIO
import libs.{ImageUtil, Blobs, Utils, Time}
import java.awt.Image
import play.Play
import java.awt.image.BufferedImage

abstract sealed class EgraphState(val value: String)

case object AwaitingVerification extends EgraphState("AwaitingVerification")
case object Verified extends EgraphState("Verified")
case object RejectedVoice extends EgraphState("Rejected:Voice")
case object RejectedSignature extends EgraphState("Rejected:Signature")
case object RejectedBoth extends EgraphState("Rejected:Both")
case object RejectedPersonalAudit extends EgraphState("Rejected:Audit")

/**
 * Persistent entity representing a single eGraph.
 *
 * An Egraph is both the final delivered product and an attempt to fulfill an order.
 */
case class Egraph(
  id: Long = 0L,
  orderId: Long = 0L,
  stateValue: String = AwaitingVerification.value,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import Blobs.Conversions._

  //
  // Public methods
  //
  def save(signature: String, audio: Array[Byte]): Egraph = {
    val saved = Egraph.save(this)

    // Have to save assets after saving the entity so that they can be
    // properly keyed
    saved.assets.save(signature, audio)

    saved
  }

  /** Fetches the related order from the db */
  def order: Order = {
    Order.get(orderId)
  }

  /**
   * Saves the entity without updating the assets. IT IS AN ERROR to call this
   * method on an Egraph that is being persisted for the first time. Only use
   * it to update an Egraph's status.
   */
  def saveWithoutAssets(): Egraph = {
    Egraph.save(this)
  }

  /** Returns a transform of this object with the new, parameterized state */
  def withState(state: EgraphState): Egraph = {
    copy(stateValue = state.value)
  }

  /** The current state of the eGraph */
  def state: EgraphState = {
    Egraph.states(stateValue)
  }

  /**
   * Returns the file assets associated with this Egraph. Throws a runtime
   * exception  if the entity couldn't possibly have assets given its default
   * id.
   */
  def assets: EgraphAssets = {
    require(id > 0L, "Can't access assets of an Egraph that lacks an ID.")

    Assets
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Egraph.unapply(this)
  }

  private object Assets extends EgraphAssets {
    //
    // EgraphAssets members
    //
    override def signature: String = {
      Blobs.get(signatureJsonKey).get.asString
    }
    
    override def audio: Stream[Byte] = {
      Blobs.get(audioKey).get.asByteStream
    }

    override def audioUrl = {
      Blobs.getUrl(audioKey)
    }

    override def image: ImageAsset = {
      ImageAsset(blobKeyBase, imageName, ImageAsset.Png)
    }

    override def save(signature: String, audio: Array[Byte]) {
      Blobs.put(signatureJsonKey, signature, access=AccessPolicy.Private)
      Blobs.put(audioKey, audio, access=AccessPolicy.Public)

      // Before removing this line, realize that without the line the image method will fail
      ImageAsset(
        createMasterImage(signature, order.product.photo.renderFromMaster),
        blobKeyBase,
        imageName,
        ImageAsset.Png
      ).save(AccessPolicy.Public)
    }

    lazy val audioKey = blobKeyBase + "/audio.wav"
    lazy val signatureJsonKey = signatureKey + ".json"

    //
    // Private members
    //
    private lazy val blobKeyBase = "egraphs/" + id
    private lazy val signatureKey = blobKeyBase + "/signature"
    private lazy val imageName = "image"

    private def createMasterImage(sig: String = this.signature,
                                  productImage: BufferedImage):Array[Byte] =
    {
      import ImageUtil.Conversions._

      val signatureImage = ImageUtil.createSignatureImage(sig)

      ImageUtil.createEgraphImage(signatureImage, productImage, 0, 0).asByteArray(ImageAsset.Png)
    }

  }
}

/**
 * Visual and audio assets associated with the Egraph entity. These are stored in the blobstore
 * rather than the relational database. Access this class via the assets method
 */
trait EgraphAssets {
  /**
   * Retrieves the signature json from the blobstore.
   */
  def signature: String

  /**
   * Retrieves the bytes of audio from the blobstore.
   */
  def audio: Stream[Byte]

  /**
   * Retrieves the url of the audio in the blobstore.
   */
  def audioUrl: String

  def image: ImageAsset

  /** Stores the assets in the blobstore */
  def save(signature: String, audio: Array[Byte])
}

object Egraph extends Saves[Egraph] with SavesCreatedUpdated[Egraph] {
  //
  // Public Methods
  //
  /** Map of Egraph state strings to the actual EgraphStates */
  val states = Utils.toMap[String, EgraphState](Seq(
    AwaitingVerification,
    Verified,
    RejectedVoice,
    RejectedSignature,
    RejectedBoth,
    RejectedPersonalAudit
  ), key=(theState) => theState.value)

  //
  // Saves[Egraph] methods
  //
  override val table = Schema.egraphs

  override def defineUpdate(theOld: Egraph, theNew: Egraph) = {
    import org.squeryl.PrimitiveTypeMode._

    updateIs(
      theOld.orderId := theNew.orderId,
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