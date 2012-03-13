package models

import java.sql.Timestamp
import services.blobs.AccessPolicy
import services.{ImageUtil, Utils, Time}
import java.awt.image.BufferedImage
import com.google.inject.Inject
import services.AppConfig
import services.blobs.Blobs
import services.db.{Schema, KeyedCaseClass, Saves}
import org.jclouds.blobstore.domain.Blob
import vbg.VBGVerifySample
import services.voice.{VoiceBiometricsError, YesMaamVoiceBiometricService, VoiceBiometricService}
import xyzmo.XyzmoVerifyUser
import services.signature.{SignatureBiometricsError, YesMaamSignatureBiometricService, SignatureBiometricService}

case class EgraphServices @Inject() (
  store: EgraphStore,
  celebStore: CelebrityStore,
  orderStore: OrderStore,
  images: ImageUtil,
  blobs: Blobs,
  voiceBiometrics: VoiceBiometricService,
  signatureBiometrics: SignatureBiometricService,
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
  stateValue: String = EgraphState.AwaitingVerification.value,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: EgraphServices = AppConfig.instance[EgraphServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import Blobs.Conversions._

  //
  // Public methods
  //
  def save(): Egraph = {
    services.store.save(this)
  }

  def withAssets(signature: String, message: Option[String] = None, audio: Array[Byte]): EgraphWithAssets = {
    EgraphWithAssets(this, signature, message, audio)
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
    EgraphState.named(stateValue)
  }

  /**
   * Returns a copy of this Egraph where the biometric services used by the Egraph
   * is a version that always verifies all requests.
   */
  def withYesMaamBiometricServices: Egraph = {
    val niceServices = services.copy(
      voiceBiometrics = new YesMaamVoiceBiometricService,
      signatureBiometrics = new YesMaamSignatureBiometricService
    )

    this.copy(services=niceServices)
  }

  def celebrity: Celebrity = {
    services.celebStore.findByEgraphId(id).get
  }

  /**
   * Verifies the celebrity against a voice profile
   */
  def verifyVoice: Option[VBGVerifySample] = {
    val voiceResponse: Either[VoiceBiometricsError, VBGVerifySample] = services.voiceBiometrics.verify(this)
    if (voiceResponse.isRight)
      Some(voiceResponse.right.get)
    else
      None
  }

  /**
   * Verifies the celebrity against a signature profile
   */
  def verifySignature: Option[XyzmoVerifyUser] = {
    val signatureResponse: Either[SignatureBiometricsError, XyzmoVerifyUser] = services.signatureBiometrics.verify(this)
    if (signatureResponse.isRight)
      Some(signatureResponse.right.get)
    else
      None
  }

  def verifyBiometrics: Egraph = {
    val vbgVerifySample: Option[VBGVerifySample] = verifyVoice
    val xyzmoVerifyUser: Option[XyzmoVerifyUser] = verifySignature
    withRecalculatedStatus(vbgVerifySample = vbgVerifySample, xyzmoVerifyUser = xyzmoVerifyUser)
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

  /**
   * Recalculates the correct EgraphState for the egraph based on the dependent variables
   * from its biometrics calculation.
   */
  private def withRecalculatedStatus(vbgVerifySample: Option[VBGVerifySample], xyzmoVerifyUser: Option[XyzmoVerifyUser]): Egraph = {
    import EgraphState._

    val newState = (xyzmoVerifyUser, vbgVerifySample) match {

      // We're awaiting verification if we don't have any value yet for either of the two biometric services
      case (None, _) | (_, None)=>
        AwaitingVerification

      // We have return codes from both services. Taking a look at them...
      case (Some(signatureResult), Some(voiceResult)) =>
        Verified // TODO: Remove for April 1 release and write unit tests for the logic below
        /*
        val isSignatureMatch = signatureResult.isMatch.getOrElse(false)
        val isVoiceMatch = voiceResult.success.getOrElse(false)
        (isSignatureMatch, isVoiceMatch) match {
          case (true, true) => Verified
          case (true, false) => RejectedVoice
          case (false, true) => RejectedSignature
          case (false, false) => RejectedBoth
        }
      */
    }

    this.withState(newState)
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

    override def audio: Blob = {
      blobs.get(audioKey).get
    }

    override def message: Option[String] = {
      blobs.get(messageJsonKey).flatMap { messageBlob =>
        Some(messageBlob.asString)
      }
    }

    override def audioUrl = {
      blobs.getUrl(audioKey)
    }

    override def image: ImageAsset = {
      ImageAsset(blobKeyBase, imageName, ImageAsset.Png, services.imageAssetServices)
    }

    override def save(signature: String, message: Option[String], audio: Array[Byte]) {
      blobs.put(signatureJsonKey, signature, access=AccessPolicy.Private)
      blobs.put(audioKey, audio, access=AccessPolicy.Public)

      // Put in the message if it was provided
      message.foreach { messageString =>
        blobs.put(messageJsonKey, messageString, access=AccessPolicy.Private)
      }

      // Before removing this line, realize that without the line the image method will fail
      ImageAsset(
        createMasterImage(signature, message, order.product.photo.renderFromMaster),
        blobKeyBase,
        imageName,
        ImageAsset.Png,
        services.imageAssetServices
      ).save(AccessPolicy.Public)
    }

    lazy val audioKey = blobKeyBase + "/audio.wav"
    lazy val signatureJsonKey = signatureKey + ".json"
    lazy val messageJsonKey = messageKey + ".json"

    //
    // Private members
    //
    private lazy val blobKeyBase = "egraphs/" + id
    private lazy val signatureKey = blobKeyBase + "/signature"
    private lazy val messageKey = blobKeyBase + "/message"
    private lazy val imageName = "image"

    private def createMasterImage(sig: String = this.signature,
                                  message: Option[String] = this.message,
                                  productImage: BufferedImage):Array[Byte] =
    {
      import ImageUtil.Conversions._

      val signatureImage = services.images.createSignatureImage(sig, message)

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
   * Retrieves the message json from the blobstore -- this is the message written by the
   * celebrity for the recipient in json vector format.
   */
  def message: Option[String]

  /**
   * Retrieves the bytes of audio from the blobstore.
   */
  def audio: Blob

  /**
   * Retrieves the url of the audio in the blobstore.
   */
  def audioUrl: String

  def image: ImageAsset

  /** Stores the assets in the blobstore */
  def save(signature: String, message: Option[String], audio: Array[Byte])
}

class EgraphStore @Inject() (schema: Schema) extends Saves[Egraph] with SavesCreatedUpdated[Egraph] {
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

case class EgraphWithAssets(
  egraph: Egraph,
  signature: String,
  message: Option[String],
  audio: Array[Byte])
{
  def save(): Egraph = {
    val savedEgraph = egraph.save()

    savedEgraph.assets.save(signature, message, audio)

    savedEgraph
  }
}

abstract sealed class EgraphState(val value: String)

object EgraphState {
  case object AwaitingVerification extends EgraphState("AwaitingVerification")
  case object Verified extends EgraphState("Verified")
  case object RejectedVoice extends EgraphState("Rejected:Voice")
  case object RejectedSignature extends EgraphState("Rejected:Signature")
  case object RejectedBoth extends EgraphState("Rejected:Both")
  case object RejectedPersonalAudit extends EgraphState("Rejected:Audit")

  /** Map of Egraph state strings to the actual EgraphStates */
  val named = Utils.toMap[String, EgraphState](Seq(
    AwaitingVerification,
    Verified,
    RejectedVoice,
    RejectedSignature,
    RejectedBoth,
    RejectedPersonalAudit
  ), key=(theState) => theState.value)
}
