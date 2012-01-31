package models

import java.sql.Timestamp
import db.{KeyedCaseClass, Saves}
import services.blobs.AccessPolicy
import services.{ImageUtil, Utils, Time}
import java.awt.image.BufferedImage
import com.google.inject.Inject
import services.AppConfig
import services.blobs.Blobs

abstract sealed class EgraphState(val value: String)

case object AwaitingVerification extends EgraphState("AwaitingVerification")
case object Verified extends EgraphState("Verified")
case object RejectedVoice extends EgraphState("Rejected:Voice")
case object RejectedSignature extends EgraphState("Rejected:Signature")
case object RejectedBoth extends EgraphState("Rejected:Both")
case object RejectedPersonalAudit extends EgraphState("Rejected:Audit")

case class EgraphServices @Inject() (
  store: EgraphStore,
  orderStore: OrderStore,
  images: ImageUtil,
  blobs: Blobs,
  imageAssetServices: ImageAssetServices
)

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
  updated: Timestamp = Time.defaultTimestamp,
  services: EgraphServices = AppConfig.instance[EgraphServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import Blobs.Conversions._

  //
  // Public methods
  //
  def save(signature: String, audio: Array[Byte]): Egraph = {
    val saved = services.store.save(this)

    // Have to save assets after saving the entity so that they can be
    // properly keyed
    saved.assets.save(signature, audio)

    saved
  }

  /** Fetches the related order from the db */
  def order: Order = {
    services.orderStore.get(orderId)
  }

  /**
   * Saves the entity without updating the assets. IT IS AN ERROR to call this
   * method on an Egraph that is being persisted for the first time. Only use
   * it to update an Egraph's status.
   */
  def saveWithoutAssets(): Egraph = {
    services.store.save(this)
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
    val blobs = services.blobs

    //
    // EgraphAssets members
    //
    override def signature: String = {
      blobs.get(signatureJsonKey).get.asString
    }
    
    override def audio: Stream[Byte] = {
      blobs.get(audioKey).get.asByteStream
    }

    override def audioUrl = {
      blobs.getUrl(audioKey)
    }

    override def image: ImageAsset = {
      ImageAsset(blobKeyBase, imageName, ImageAsset.Png, services.imageAssetServices)
    }

    override def save(signature: String, audio: Array[Byte]) {
      blobs.put(signatureJsonKey, signature, access=AccessPolicy.Private)
      blobs.put(audioKey, audio, access=AccessPolicy.Public)

      // Before removing this line, realize that without the line the image method will fail
      ImageAsset(
        createMasterImage(signature, order.product.photo.renderFromMaster),
        blobKeyBase,
        imageName,
        ImageAsset.Png,
        services.imageAssetServices
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

      val signatureImage = services.images.createSignatureImage(sig)

      services.images.createEgraphImage(signatureImage, productImage, 0, 0).asByteArray(ImageAsset.Png)
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

class EgraphStore @Inject() (schema: db.Schema) extends Saves[Egraph] with SavesCreatedUpdated[Egraph] {
  //
  // Saves[Egraph] methods
  //
  override val table = schema.egraphs

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

object Egraph {
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

}