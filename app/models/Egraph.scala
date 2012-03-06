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
import services.signature.{YesMaamSignatureBiometricService, SignatureBiometricService}
import services.voice.{YesMaamVoiceBiometricService, VoiceBiometricService, VoiceBiometricsCode}

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
  // todo(wchan): Remove these now denormalized columns that exist on XyzmoVerifyUser and VBGVerifySample
  _voiceCode: Option[String] = None,
  _voiceSuccess: Option[Boolean] = None,
  _voiceScore: Option[Long] = None,
  _signatureCode: Option[String] = None,
  _signatureSuccess: Option[Boolean] = None,
  _signatureScore: Option[Int] = None,
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
  def verifyVoice: Egraph = {
    val voiceResponse = services.voiceBiometrics.verify(assets.audio.asByteArray, this)
    val (voiceCode, voiceSuccess, voiceScore) = voiceResponse.fold(
      error =>
        (Some(error.code), None,  None),

      verification =>
        (Some(VoiceBiometricsCode.Success.name), Some(verification.success), Some(verification.score))
    )

    val egraphWithVoiceMetadata = this.copy(
      _voiceCode = voiceCode,
      _voiceSuccess = voiceSuccess,
      _voiceScore = voiceScore
    )

    egraphWithVoiceMetadata.withRecalculatedStatus
  }

  /**
   * Verifies the celebrity against a signature profile. Returns the celebrity with the
   * properly
   */
  def verifySignature: Egraph = {
    val signatureResponse = services.signatureBiometrics.verify(assets.signature, this)
    val (signatureCode, signatureSuccess, signatureScore) = signatureResponse.fold (
      error =>
        // TODO(erem): put the actual Xyzmo codes in here
        (Some("Failure"), None, None),

      verification =>
        (Some("Success"), Some(verification.success), verification.score)
    )

    val egraphWithSignatureMetadata = this.copy(
      _signatureCode = signatureCode,
      _signatureSuccess = signatureSuccess,
      _signatureScore = signatureScore
    )

    egraphWithSignatureMetadata.withRecalculatedStatus
  }

  def verifyBiometrics: Egraph = {
    val thisWithVoiceVerified: Egraph = verifyVoice
    thisWithVoiceVerified.verifySignature
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
  private def withRecalculatedStatus: Egraph = {
    import EgraphState._

    val newState = (_signatureCode, _voiceCode) match {
      // We're awaiting verification if we don't have any value yet for either of the two biometrics
      case (None, _) | (_, None)=>
        AwaitingVerification

      // We have return codes from both services. Taking a look at them...
      case (Some(signatureCode), Some(voiceCode)) =>
        Verified // TODO: Remove for April 1 release
        /*
        (signatureCode, voiceCode) match {
          // Both service requests occurred without error. Looking at the algorithm results.
          // TODO(erem): Replace "Success" here with the actual xyzmo value
          case ("Success", VoiceBiometricsCode.Success.name) =>
            (_signatureSuccess.get, _voiceSuccess.get) match {
              case (true, true) => Verified
              case (true, false) => RejectedVoice
              case (false, true) => RejectedSignature
              case (false, false) => RejectedBoth
            }

          // These three cases only ever happen if at least one service errored out
          // rather than returning a value
          case (_, VoiceBiometricsCode.Success.name) => RejectedSignature
          case ("Success", _) => RejectedVoice
          case (_, _) => RejectedBoth
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
      theOld._voiceCode := theNew._voiceCode,
      theOld._voiceSuccess := theNew._voiceSuccess,
      theOld._voiceScore := theNew._voiceScore,
      theOld._signatureCode := theNew._signatureCode,
      theOld._signatureSuccess := theNew._signatureSuccess,
      theOld._signatureScore := theNew._signatureScore,
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
