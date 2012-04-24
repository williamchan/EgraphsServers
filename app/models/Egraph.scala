package models

import java.sql.Timestamp
import services.blobs.AccessPolicy
import java.awt.image.BufferedImage
import services.blobs.Blobs
import org.jclouds.blobstore.domain.Blob
import services.voice.{VoiceBiometricsError, YesMaamVoiceBiometricService, VoiceBiometricService}
import vbg.{VBGVerifySampleStore, VBGVerifySample}
import services.signature.{SignatureBiometricsError, YesMaamSignatureBiometricService, SignatureBiometricService}
import services._
import db.{FilterOneTable, Schema, KeyedCaseClass, Saves}
import java.text.SimpleDateFormat
import controllers.WebsiteControllers
import controllers.website.GetCelebrityProductEndpoint
import com.google.inject.{Provider, Inject}
import play.utils.HTML.htmlEscape
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import models.Egraph.EgraphState
import xyzmo.{XyzmoVerifyUserStore, XyzmoVerifyUser}

case class EgraphServices @Inject() (
  store: EgraphStore,
  celebStore: CelebrityStore,
  orderStore: OrderStore,
  vbgVerifySampleStore: VBGVerifySampleStore,
  xyzmoVerifyUserStore: XyzmoVerifyUserStore,
  images: ImageUtil,
  blobs: Blobs,
  voiceBiometrics: VoiceBiometricService,
  signatureBiometrics: SignatureBiometricService,
  imageAssetServices: ImageAssetServices,
  storyServicesProvider: Provider[EgraphStoryServices]
)

/**
 * Persistent entity representing a single Egraph.
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
  import EgraphState._

  //
  // Public methods
  //
  /**
   * Returns the "story" for the egraph, which is publicly displayed on the egraph's page.
   *
   * @param signer the celebrity that signed the egraph
   * @param product the photographic product signed by the celebrity
   * @param order the order opened by the buying customer.
   *
   * @return an EgraphStory that can be used to safely render the story into an HTML page,
   *   unescaped.
   */
  def story(signer: Celebrity, product: Product, order: Order): EgraphStory = {
    EgraphStory(
      titleTemplate=product.storyTitle,
      bodyTemplate=product.storyText,
      celebName=signer.publicName.get,
      celebUrlSlug=signer.urlSlug.get,
      recipientName=order.recipientName,
      productName=product.name,
      productUrlSlug=product.urlSlug,
      orderTimestamp=order.created,
      signingTimestamp=this.created,
      services=services.storyServicesProvider.get()
    )
  }

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

  /** The current state of the Egraph */
  def state: EgraphState = {
    EgraphState.all(stateValue)
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

  def signatureResult: Option[XyzmoVerifyUser] = {
    services.xyzmoVerifyUserStore.findByEgraph(this)
  }

  def voiceResult: Option[VBGVerifySample] = {
    services.vbgVerifySampleStore.findByEgraph(this)
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

  def isApprovable: Boolean = {
    state == PassedBiometrics || state == FailedBiometrics
  }

  def approve(admin: Administrator): Egraph = {
    require(admin != null, "Must be approved by an Administrator")
    require(isApprovable, "Must have previously been checked by biometrics")
    withState(ApprovedByAdmin)
  }

  def reject(admin: Administrator): Egraph = {
    require(admin != null, "Must be rejected by an Administrator")
    require(isApprovable, "Must have previously been checked by biometrics")
    withState(RejectedByAdmin)
  }

  def isPublishable: Boolean = {
    state == ApprovedByAdmin
  }

  def publish(admin: Administrator): Egraph = {
    require(admin != null, "Must be rejected by an Administrator")
    require(isPublishable, "Must have previously been approved by admin")
    withState(Published)
  }

  /**
   * Recalculates the correct EgraphState for the egraph based on the dependent variables
   * from its biometrics calculation.
   */
  private def withRecalculatedStatus(vbgVerifySample: Option[VBGVerifySample], xyzmoVerifyUser: Option[XyzmoVerifyUser]): Egraph = {
    val newState = (xyzmoVerifyUser, vbgVerifySample) match {

      // We're awaiting verification if we don't have any value yet for either of the two biometric services
      case (None, _) | (_, None)=>
        AwaitingVerification

      // We have return codes from both services. Taking a look at them...
      case (Some(signatureResult), Some(voiceResult)) =>
        Published // TODO: Remove for April 1 release and write unit tests for the logic below
        /*
        val isSignatureMatch = signatureResult.isMatch.getOrElse(false)
        val isVoiceMatch = voiceResult.success.getOrElse(false)
        (isSignatureMatch, isVoiceMatch) match {
          case (true, true) => PassedBiometrics
          case _ => FailedBiometrics
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
    }

    override def initMasterImage {
      import Blobs.Conversions._
      val signatureBlob = blobs.get(signatureJsonKey)
      val signature = if (signatureBlob.isDefined) signatureBlob.get.asString else ""
      val messageBlob = blobs.get(messageJsonKey)
      val message = if (messageBlob.isDefined) Some(messageBlob.get.asString) else None

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

object Egraph {
  abstract sealed class EgraphState(val value: String)

  object EgraphState {
    case object AwaitingVerification extends EgraphState("AwaitingVerification")
    case object Published extends EgraphState("Published")
    case object PassedBiometrics extends EgraphState("PassedBiometrics")
    case object FailedBiometrics extends EgraphState("FailedBiometrics")
    case object ApprovedByAdmin extends EgraphState("ApprovedByAdmin")
    case object RejectedByAdmin extends EgraphState("RejectedByAdmin")

    /** Map of Egraph state strings to the actual EgraphStates */
    val all = Utils.toMap[String, EgraphState](Seq(
      AwaitingVerification,
      Published,
      PassedBiometrics,
      FailedBiometrics,
      ApprovedByAdmin,
      RejectedByAdmin
    ), key=(theState) => theState.value)
  }
}

/**
 * Service interfaces used by the EgraphStory.
 *
 * @param templateEngine the templating engine used to provide user-side templating.
 */
case class EgraphStoryServices @Inject() (templateEngine: TemplateEngine)

/**
 * The set of fields available for users to address when writing their stories nad
 * story titles. This permits them to provide the following type of narratives for the
 * story, which will get interpreted on the fly by the Egraph page.
 *
 * {{{
 *  Everybody wants a piece of {start-celebrity-link}{signer-name}{end-link}'s fame.
 *  But only {recipient-name} got it. Because of Egraphs.
 * }}}
 *
 */
object EgraphStoryField extends Utils.Enum {
  sealed trait EnumVal extends Value

  /** Public name of the celebrity */
  val CelebrityName = new EnumVal { val name = "signer_name" }

  /**
   * Begins a link to the celebrity's page. Must be closed by an
   * [[models.EgraphStoryField.FinishLink]]
   * */
  val StartCelebrityLink = new EnumVal { val name = "signer_link"}

  /** Name of the person receiving the egraph */
  val RecipientName = new EnumVal { val name = "recipient_name"}

  /** Name of the product being sold */
  val ProductName = new EnumVal { val name = "product_name"}

  /**
   * Begins a link to the photographic product. Must be closed by a
   * [[models.EgraphStoryField.FinishLink]]
   **/
  val StartProductLink = new EnumVal { val name="product_link"}

  /** Prints the date the Egraph was ordered. */
  val DateOrdered = new EnumVal { val name = "date_ordered"}

  /** Prints the date the Egraph was signed */
  val DateSigned = new EnumVal { val name = "date_signed"}

  /** Closes the last opened link */
  val FinishLink = new EnumVal { val name="end_link" }
}

/**
 * Represents the story of an egraph, as presented on the egraph page.
 *
 * @param titleTemplate title template as specified on the [[models.Product]]
 * @param bodyTemplate title template as specified on the [[models.bodyTemplate]]
 * @param celebName the celebrity's public name
 * @param celebUrlSlug see [[models.Celebrity.urlSlug]]
 * @param recipientName name of the [[models.Customer]] receiving the egraph.
 * @param productName name of the purchased [[models.Product]]
 * @param productUrlSlug see [[models.Product.urlSlug]]
 * @param orderTimestamp the moment the buying [[models.Customer]] ordered the [[models.Product]]
 * @param signingTimestamp the moment the [[models.Celebrity]] fulfilled the [[models.Order]]
 * @param services
 */
case class EgraphStory(
  private val titleTemplate: String,
  private val bodyTemplate: String,
  private val celebName: String,
  private val celebUrlSlug: String,
  private val recipientName: String,
  private val productName: String,
  private val productUrlSlug: String,
  private val orderTimestamp: Timestamp,
  private val signingTimestamp: Timestamp,
  private val services: EgraphStoryServices = AppConfig.instance[EgraphStoryServices]
) {

  //
  // Public methods
  //
  /** Returns the story title */
  def title: String = {
    services.templateEngine.evaluate(htmlEscape(titleTemplate), templateParams)
  }

  /** Returns the body of the story */
  def body: String = {
    services.templateEngine.evaluate(htmlEscape(bodyTemplate), templateParams)
  }

  //
  // Private methods
  //
  private val templateParams: Map[String, String] = {
    import EgraphStoryField._
    val pairs = for (templateField <- EgraphStoryField.values) yield {
      val paramValue = templateField match {
        case CelebrityName => celebName
        case StartCelebrityLink => startCelebPageLink
        case RecipientName => recipientName
        case ProductName => productName
        case StartProductLink => startProductLink
        case DateOrdered => formatTimestamp(orderTimestamp)
        case DateSigned => formatTimestamp(signingTimestamp)
        case FinishLink => "</a>"
      }

      (templateField.name, paramValue)
    }

    pairs.toMap
  }

  private def dateFormat = {
    new SimpleDateFormat("MMMM dd, yyyy")
  }

  private def startCelebPageLink: String = {
    htmlAnchorStart(href=WebsiteControllers.lookupGetCelebrity(celebUrlSlug).url)
  }

  private def startProductLink: String = {
    htmlAnchorStart(
      href=GetCelebrityProductEndpoint.urlFromSlugs(celebUrlSlug, productUrlSlug).url
    )
  }

  private def htmlAnchorStart(href: String) = {
    "<a href='" + href + "' >"
  }

  private def formatTimestamp(timestamp: Timestamp): String = {
    dateFormat.format(timestamp)
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

  /** Must be called after initImageAsset. */
  def image: ImageAsset

  /** Stores the assets in the blobstore */
  def save(signature: String, message: Option[String], audio: Array[Byte])

  /** Initializes the MasterImage. Must be called after save, which saves raw data for this Egraph. */
  def initMasterImage()
}

class EgraphStore @Inject() (schema: Schema) extends Saves[Egraph] with SavesCreatedUpdated[Egraph] {

  def getEgraphsAndResults(filters: FilterOneTable[Egraph]*): Query[(Egraph, Option[VBGVerifySample], Option[XyzmoVerifyUser])] = {
    join(schema.egraphs, schema.vbgVerifySampleTable.leftOuter, schema.xyzmoVerifyUserTable.leftOuter)(
      (egraph, vbgVerifySample, xyzmoVerifyUser) =>
        where(FilterOneTable.reduceFilters(filters, egraph))
          select(egraph, vbgVerifySample, xyzmoVerifyUser)
          orderBy (egraph.id desc)
          on(egraph.id === vbgVerifySample.map(_.egraphId), egraph.id === xyzmoVerifyUser.map(_.egraphId))
    )
  }

  def getCelebrityEgraphsAndResults(celebrity: Celebrity, filters: FilterOneTable[Egraph]*): Query[(Egraph, Option[VBGVerifySample], Option[XyzmoVerifyUser])] = {
    val celebrityId = celebrity.id
    join(schema.egraphs, schema.vbgVerifySampleTable.leftOuter, schema.xyzmoVerifyUserTable.leftOuter, schema.orders, schema.products)(
      (egraph, vbgVerifySample, xyzmoVerifyUser, order, product) =>
        where(FilterOneTable.reduceFilters(filters, egraph) and product.celebrityId === celebrityId)
          select(egraph, vbgVerifySample, xyzmoVerifyUser)
          orderBy (egraph.id desc)
          on(egraph.id === vbgVerifySample.map(_.egraphId), egraph.id === xyzmoVerifyUser.map(_.egraphId), egraph.orderId === order.id, order.productId === product.id)
    )
  }

  //
  // Saves[Egraph] methods
  //
  override val table = schema.egraphs

  override def defineUpdate(theOld: Egraph, theNew: Egraph) = {
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

class EgraphQueryFilters @Inject() (schema: Schema) {

  import EgraphState._

  def pendingAdminReview: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue in Seq(PassedBiometrics.value, FailedBiometrics.value, ApprovedByAdmin.value))
      }
    }
  }

  def approvedByAdmin: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue === ApprovedByAdmin.value)
      }
    }
  }

  def rejectedByAdmin: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue === RejectedByAdmin.value)
      }
    }
  }

  def passedBiometrics: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue === PassedBiometrics.value)
      }
    }
  }

  def failedBiometrics: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue === FailedBiometrics.value)
      }
    }
  }

  def awaitingVerification: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue === AwaitingVerification.value)
      }
    }
  }

  def published: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph.stateValue === Published.value)
      }
    }
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

/**
 * Specifies the representation of an egraph on the egraph page as well
 * as guidelines in the form of an aspect ratio on how to display the egraph
 * in settings other than the egraph page.
 *
 * See https://egraphs.jira.com/wiki/display/DEV/Egraph+Page#EgraphPage-ImageSpecifications
 * for more about egraph page image layout.
 **/
sealed trait EgraphFrame {
  //
  // Abstract members
  //
  /** Name of the frame. This should be unique among all Frames */
  def name: String

  /**
   * The class represented by this frame in egraph.less or any other
   * included stylesheet.
   **/
  def cssClass: String

  /**
   * Twitter Bootstrap classes that specify the width of the egraph page
   * portion occupied by the frame. e.g. "offset1 span5"
   *
   */
  def cssFrameColumnClasses: String

  /**
   * Twitter Bootstrap classes that specify the width of the egraph page
   * potion occupied by the story. e.g. "offset1 span5"
   */
  def cssStoryColumnClasses: String

  /** Width of the image in pixels as displayed on the egraph page */
  def imageWidthPixels: Int

  /** Height of the image in pixels as displayed on the egraph page */
  def imageHeightPixels: Int

  //
  // Implemented members
  //
  /**
   * Returns the aspect ratio of the image: width / height. It is returned with specificity
   * to the 1e-4th decimal place.
   * @return
   */
  def imageAspectRatio: Double = {
    val rawRatio = imageWidthPixels.toDouble / imageHeightPixels.toDouble

    (rawRatio * 10000).round / 10000.0
  }

  /**
   * Returns a copy of an image of arbitrary dimensions, cropped so that it
   * will fit in the frame once resized to imageWidthPixels by imageHeightPixels.
   *
   * Returns the cropped image.
   *
   * @param image
   * @return a cropped copy of the image argument.
   */
  def cropImageForFrame(image: BufferedImage): BufferedImage = {
    val targetAspectRatio = this.imageAspectRatio
    val originalWidth = image.getWidth.toDouble
    val originalHeight = image.getHeight.toDouble
    val originalAspectRatio = originalWidth / originalHeight

    val cropDimensions = if (originalAspectRatio < targetAspectRatio) {
      // the original is too tall. Use all of width and limit height.
      Dimensions(width=originalWidth.toInt, height=(originalWidth / targetAspectRatio).toInt)
    } else {
      // the original is too narrow. Use all of height and limit width.
      Dimensions(width=(originalHeight * targetAspectRatio).toInt, height=originalHeight.toInt)
    }

    ImageUtil.crop(image, cropDimensions)
  }
}

object EgraphFrame {
  /**
   * Returns the suggested frame for a given image. Decision is made based on frame
   * dimensions
   **/
  private def suggestedFrameForDimensions(pixelWidth: Int, pixelHeight: Int): EgraphFrame = {
    if (pixelWidth > pixelHeight) LandscapeEgraphFrame else PortraitEgraphFrame
  }

  /** See suggestedFrameForDimensions */
  def suggestedFrame(dimensions: Dimensions): EgraphFrame = {
    suggestedFrameForDimensions(dimensions.width, dimensions.height)
  }
}

/** The default egraph portrait frame */
object PortraitEgraphFrame extends EgraphFrame {
  override val name: String = "Default Portrait"

  override val cssClass  = "portrait"
  override val cssFrameColumnClasses = "offset1 span6"
  override val cssStoryColumnClasses = "span5"

  override val imageWidthPixels = 377
  override val imageHeightPixels = 526
}

/** The default egraph landscape photo frame */
object LandscapeEgraphFrame extends EgraphFrame {
  override val name = "Default Landscape"

  override val cssClass  = "landscape"
  override val cssFrameColumnClasses = "span8"
  override val cssStoryColumnClasses = "span3"

  override val imageWidthPixels = 595
  override val imageHeightPixels = 377
}