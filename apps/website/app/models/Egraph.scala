package models

import enums.{HasEgraphState, EgraphState}
import java.sql.Timestamp
import services.blobs.AccessPolicy
import java.awt.image.BufferedImage
import services.blobs.Blobs
import org.jclouds.blobstore.domain.Blob
import services.voice.{VoiceBiometricsError, YesMaamVoiceBiometricService, VoiceBiometricService}
import vbg.{VBGVerifySampleStore, VBGVerifySample}
import services.signature.{SignatureBiometricsError, YesMaamSignatureBiometricService, SignatureBiometricService}
import services._
import audio.AudioConverter
import db.{FilterOneTable, Schema, KeyedCaseClass, SavesWithLongKey}
import graphics.{RasterGraphicsSource, Handwriting, HandwritingPen, GraphicsSource}
import com.google.inject.{Provider, Inject}
import org.squeryl.Query
import print.{StandaloneCertificatePrint, LandscapeFramedPrint}
import xyzmo.{XyzmoVerifyUserStore, XyzmoVerifyUser}
import java.util.Date

/**
 * Vital services for an Egraph to perform its necessary functionality
 *
 * @param store Egraph object persistence
 * @param celebStore Celebrity persistence
 * @param orderStore Order persistence
 * @param vbgVerifySampleStore Persistence for voice biometric scores
 * @param xyzmoVerifyUserStore Persistence for handwriting biometric scodes
 * @param blobs Persistence for handwriting JSON and audio binary
 * @param graphicsSourceFactory Source for creating a canvas for drawing the egraph.
 * @param voiceBiometrics Service for performing tests against our voice identification algorithm
 * @param signatureBiometrics Service for performing tests against our signature identification algorithm
 * @param storyServicesProvider Services needed to instantiate a fully functional EgraphStory
 */
case class EgraphServices @Inject() (
  store: EgraphStore,
  celebStore: CelebrityStore,
  orderStore: OrderStore,
  egraphQueryFilters: EgraphQueryFilters,
  vbgVerifySampleStore: VBGVerifySampleStore,
  xyzmoVerifyUserStore: XyzmoVerifyUserStore,
  blobs: Blobs,
  graphicsSourceFactory: () => GraphicsSource,
  rasterGraphicsSourceFactory: () => RasterGraphicsSource,
  voiceBiometrics: VoiceBiometricService,
  signatureBiometrics: SignatureBiometricService,
  storyServicesProvider: Provider[EgraphStoryServices]
)

/**
 * Persistent entity representing a single Egraph.
 *
 * An Egraph is an attempt to fulfill an order. When it is published it also becomes the
 * final resource consumed by the recipient.
 *
 * @param id A unique ID for each attempt to fulfill an order
 * @param orderId the order ID that this egraph fulfills
 * @param created the moment this entity was first inserted into the database
 * @param updated the last moment this entity was updated in the database
 * @param services the functionality for the Egraph to meaningfully manipulate its data.
 */
case class Egraph(
  id: Long = 0L,
  orderId: Long = 0L,
  _egraphState: String = EgraphState.AwaitingVerification.name,
  latitude: Option[Double] = None,
  longitude: Option[Double] = None,
  signedAt: Option[Timestamp] = None,         // GMT (just like all other timestamps)
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: EgraphServices = AppConfig.instance[EgraphServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasEgraphState[Egraph]
{
  import Blobs.Conversions._
  import EgraphState._

  private lazy val blobKeyBase = "egraphs/" + id
  private def imageAssetBlobKeyBase = blobKeyBase + "/image"
  private def videoAssetBlobKeyBase = blobKeyBase + "/video"

  private def framedPrintVersion = "v" + LandscapeFramedPrint.currentVersion
  private def framedPrintBlobKey = blobKeyBase + "/framed-print/" + framedPrintVersion + "/" + framedPrintFilename
  def framedPrintFilename = "order" + orderId + ".jpg" // this cannot change it is linked to printer specifications
  private def standaloneCertPrintVersion = "v" + StandaloneCertificatePrint.currentVersion
  private def standaloneCertPrintBlobKey = blobKeyBase + "/certificate/" + standaloneCertPrintVersion + "/" + standaloneCertPrintFilename
  def standaloneCertPrintFilename = "order" + orderId + "-cert.jpg"

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
   * @return an EgraphStory that can be used to safely render the story into an HTML page without
   *   being further unescaped.
   */
  def story(signer: Celebrity, product: Product, order: Order): EgraphStory = {
    EgraphStory(
      titleTemplate=product.storyTitle,
      bodyTemplate=product.storyText,
      celebName=signer.publicName,
      celebUrlSlug=signer.urlSlug,
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
   * Returns the EgraphImage that can be used to access a visually rendered version of the
   * Egraph at any dimension.
   *
   * @param productPhoto the photo upon which to sign. If this isn't provided, we will access the blobstore
   *     to find it.
   *
   * @return the EgraphImage to manipulate, save, and render the Egraph.
   */
  def image(productPhoto: => BufferedImage=order.product.photoImage):EgraphImage = {
    EgraphImage(
      ingredientFactory=imageIngredientFactory(order.product, productPhoto),
      graphicsSource=services.graphicsSourceFactory(),
      blobPath=imageAssetBlobKeyBase
    )
  }

  /**
   * Returns a function that retrieves all the necessary (expensive) data for drawing an Egraph.
   * The function will only be evaluated if the required image doesn't already exist on the blobstore.
   *
   * @param productPhoto the photo that should be "signed" by the Egraph's stored vector handwriting.
   *
   */
  private def imageIngredientFactory(product: Product, productPhoto: => BufferedImage): () => EgraphImageIngredients = {
    () => {
      val myAssets = assets
      EgraphImageIngredients(
        signatureJson=myAssets.signature,
        messageJsonOption=myAssets.message,
        pen=HandwritingPen(width=Handwriting.defaultPenWidth),
        photo=productPhoto,
        photoDimensionsWhenSigned=Dimensions(product.signingScaleW, product.signingScaleH),
        signingOriginX = product.signingOriginX,
        signingOriginY = product.signingOriginY
      )
    }
  }

  /**
   * @return url of assembled JPG for physical prints, if it was successfully or previously generated
   */
  def getFramedPrintImageUrl: String = {
    services.blobs.getUrlOption(framedPrintBlobKey) match {
      case Some(url) => url
      case None => {
        val thisOrder = order
        val product = thisOrder.product
        val celebrity = product.celebrity

        // Generate and save print-sized egraph image, and get it as a BufferedImage to be passed to LandscapeFramedPrint
        val egraphImageAsPng = getEgraphImage(LandscapeFramedPrint.targetEgraphWidth, ignoreMasterWidth=false).asPng
        egraphImageAsPng.getSavedUrl(AccessPolicy.Public)
        val egraphImage = egraphImageAsPng.transformAndRender.graphicsSource.asInstanceOf[RasterGraphicsSource].image

        val framedPrintImage = LandscapeFramedPrint().assemble(
          orderNumber = orderId.toString,
          egraphImage = egraphImage,
          teamLogoImage = product.icon.renderFromMaster,
          recipientName = thisOrder.recipientName,
          celebFullName = celebrity.publicName,
          celebCasualName = celebrity.casualName.getOrElse(celebrity.publicName),
          productName = product.name,
          signedAtDate = getSignedAt,
          egraphUrl = "https://www.egraphs.com/" + orderId
        )

        services.blobs.put(key = framedPrintBlobKey, bytes = ImageUtil.getBytes(framedPrintImage), AccessPolicy.Public)
        services.blobs.getUrl(framedPrintBlobKey)
      }
    }
  }

  /**
   * @return url of stand-alone certificate of authenticity JPG, if it was successfully or previously generated
   */
  def getStandaloneCertificateUrl: String = {
    services.blobs.getUrlOption(standaloneCertPrintBlobKey) match {
      case Some(url) => url
      case None => {
        val thisOrder = order
        val product = thisOrder.product
        val celebrity = product.celebrity

        val standaloneCertImage = StandaloneCertificatePrint().assemble(
          orderNumber = orderId.toString,
          teamLogoImage = product.icon.renderFromMaster,
          recipientName = thisOrder.recipientName,
          celebFullName = celebrity.publicName,
          celebCasualName = celebrity.casualName.getOrElse(celebrity.publicName),
          productName = product.name,
          signedAtDate = getSignedAt,
          egraphUrl = "https://www.egraphs.com/" + orderId
        )

        services.blobs.put(key = standaloneCertPrintBlobKey, bytes = ImageUtil.getBytes(standaloneCertImage), AccessPolicy.Public)
        services.blobs.getUrl(standaloneCertPrintBlobKey)
      }
    }
  }

  /**
   * @param width the desired width of the egraph image. If this width is larger than the width of the master egraph
   *              image, then the master egraph image's width will be used instead.
   * @param ignoreMasterWidth if false, then the lesser of width and the master photo's width will be used. Setting this
   *                          to false is computational expensive since the image asset needs to be retrieved and examined.
   * @return the egraph image asset
   */
  def getEgraphImage(width: Int, ignoreMasterWidth: Boolean = true): EgraphImage = {
    val product = order.product
    val rawSignedImage = image(product.photoImage)
    val targetWidth = if (ignoreMasterWidth) {
      width
    } else {
      val masterWidth = product.photoImage.getWidth
      if (masterWidth < width) masterWidth else width
    }
    rawSignedImage
      .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
      .withPenShadowOffset(Handwriting.defaultShadowOffsetX, Handwriting.defaultShadowOffsetY)
      .scaledToWidth(targetWidth)
  }

  def getVideoAsset: EgraphVideoAsset = {
    EgraphVideoAsset(blobPath = videoAssetBlobKeyBase, egraph = this)
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

  /**
   * Egraphs that are published or rejected are not pending; they will not be
   * delivered to the user. HOWEVER, Egraphs that fail biometric testing should 
   * still be shown to the user. According to Kate, a LOT of Egraphs currently
   * fail biometrics, but are published anyway. We want those Egraphs to still
   * be considered pending. In other words, the Customer should have no way of 
   * knowing whether their Egraph passed or failed biometrics.
   *
   * @return Whether an egraph is on its way to a user
   */
  def isPendingEgraph: Boolean = {
    egraphState match {
      case EgraphState.Published |
           EgraphState.RejectedByAdmin |
           EgraphState.RejectedByMlb => false
      case _ => true
    }
  }

  def isApprovable: Boolean = {
    List(PassedBiometrics, FailedBiometrics).contains(egraphState)
  }

  def isRejectable: Boolean = {
    List(PassedBiometrics, FailedBiometrics, ApprovedByAdmin, Published).contains(egraphState)
  }

  def approve(admin: Administrator): Egraph = {
    require(admin != null, "Must be approved by an Administrator")
    require(isApprovable, "Must have previously been checked by biometrics")

    val nextState = if (!celebrity.isMlb) ApprovedByAdmin else PendingMlbReview
    withEgraphState(nextState)
  }

  def reject(admin: Administrator): Egraph = {
    require(admin != null, "Must be rejected by an Administrator")
    require(isRejectable, "Must have previously been checked by biometrics or been approved")
    withEgraphState(RejectedByAdmin)
  }

  def isPublishable: Boolean = {
    egraphState == ApprovedByAdmin
  }

  def isPublished: Boolean = {
    egraphState == Published
  }

  def publish(admin: Administrator): Egraph = {
    require(admin != null, "Must be published by an Administrator")
    require(isPublishable, "Must have previously been approved by admin")
    require((services.store.findByOrder(orderId, services.egraphQueryFilters.notRejected).size == 1), "There is another Egraph in the admin flow for the same Order")
    withEgraphState(Published)
  }

  /**
   * Convenience method to return signedAt if it exists, otherwise created
   * @return signedAt if it exists, otherwise created
   */
  def getSignedAt: Date = {
    signedAt match {
      case Some(signedAtTimestamp) => new Date(signedAtTimestamp.getTime)
      case None => new Date(created.getTime)
    }
  }

  /**
   * Recalculates the correct EgraphState for the egraph based on the dependent variables
   * from its biometrics calculation.
   */
  private def withRecalculatedStatus(vbgVerifySample: Option[VBGVerifySample], xyzmoVerifyUser: Option[XyzmoVerifyUser]): Egraph = {
    val signaturePassed = xyzmoVerifyUser match {
      case Some(signatureResult) if signatureResult.isMatch.getOrElse(false) => true
      case _ => false
    }
    val voicePassed = vbgVerifySample match {
      case Some(voiceResult) if voiceResult.success.getOrElse(false) => true
      case _ => false
    }

    val newState = if (signaturePassed && voicePassed) PassedBiometrics else FailedBiometrics
    withEgraphState(newState)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = { Egraph.unapply(this) }


  override def withEgraphState(enum: EnumVal) = { copy(_egraphState = enum.name) }

  private object Assets extends EgraphAssets {
    val blobs = services.blobs

    //
    // EgraphAssets members
    //
    override def signature: String = { blobs.get(signatureJsonKey).get.asString }

    override def audioWav: Blob = { blobs.get(wavKey).get }

    override def audioMp3: Blob = { blobs.get(mp3Key).get }

    override def audioMp3Url = {
      blobs.getUrlOption((mp3Key)) match {
        case None => {
          generateAndSaveMp3()
          blobs.getUrl((mp3Key))
        }
        case Some(url) => url
      }
    }

    /**
     * Encodes an mp3 from the wav asset and stores the mp3 to the blobstore.
     */
    override def generateAndSaveMp3() {
      val mp3 = AudioConverter.convertWavToMp3(audioWav.asByteArray, blobKeyBase)
      blobs.put(mp3Key, mp3, access=AccessPolicy.Public)
    }

    override def message: Option[String] = {
      blobs.get(messageJsonKey).flatMap { messageBlob =>
        Some(messageBlob.asString)
      }
    }

    override def save(signature: String, message: Option[String], audio: Array[Byte]) {
      blobs.put(signatureJsonKey, signature, access=AccessPolicy.Private)
      blobs.put(wavKey, audio, access=AccessPolicy.Public)

      // Put in the message if it was provided
      message.foreach { messageString =>
        blobs.put(messageJsonKey, messageString, access=AccessPolicy.Private)
      }
    }

    lazy val wavKey = blobKeyBase + "/audio.wav"
    lazy val mp3Key = blobKeyBase + "/audio.mp3"
    lazy val signatureJsonKey = signatureKey + ".json"
    lazy val messageJsonKey = messageKey + ".json"

    //
    // Private members
    //
    private lazy val signatureKey = blobKeyBase + "/signature"
    private lazy val messageKey = blobKeyBase + "/message"
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
   * Retrieves the bytes of Wav audio from the blobstore.
   */
  def audioWav: Blob

  /**
   * Retrieves the bytes of mp3 audio from the blobstore.
   * Also lazily initializes the mp3 from the wav, though EgraphActor should have handled that.
   */
  def audioMp3: Blob

  /**
   * Retrieves the url of the mp3 in the blobstore.
   * Also lazily initializes the mp3 from the wav, though EgraphActor should have handled that.
   */
  def audioMp3Url: String

  /**
   * Encodes an mp3 from the wav asset and stores the mp3 to the blobstore.
   */
  def generateAndSaveMp3()

  /** Stores the assets in the blobstore */
  def save(signature: String, message: Option[String], audio: Array[Byte])
}

class EgraphStore @Inject() (schema: Schema) extends SavesWithLongKey[Egraph] with SavesCreatedUpdated[Egraph] {
  import org.squeryl.PrimitiveTypeMode._

  def getEgraphsAndResults(filters: FilterOneTable[Egraph]*): Query[(Egraph, Celebrity, Option[VBGVerifySample], Option[XyzmoVerifyUser])] = {
    join(schema.egraphs, schema.vbgVerifySampleTable.leftOuter, schema.xyzmoVerifyUserTable.leftOuter,
      schema.orders, schema.products, schema.celebrities)(
      (egraph, vbgVerifySample, xyzmoVerifyUser, order, product, celebrity) =>
        where(FilterOneTable.reduceFilters(filters, egraph))
          select(egraph, celebrity, vbgVerifySample, xyzmoVerifyUser)
          orderBy (egraph.id desc)
          on(egraph.id === vbgVerifySample.map(_.egraphId), egraph.id === xyzmoVerifyUser.map(_.egraphId),
          egraph.orderId === order.id, order.productId === product.id, product.celebrityId === celebrity.id)
    )
  }

  def getCelebrityEgraphsAndResults(celebrity: Celebrity, filters: FilterOneTable[Egraph]*): Query[(Egraph, Celebrity, Option[VBGVerifySample], Option[XyzmoVerifyUser])] = {
    val celebrityId = celebrity.id
    join(schema.egraphs, schema.vbgVerifySampleTable.leftOuter, schema.xyzmoVerifyUserTable.leftOuter,
      schema.orders, schema.products, schema.celebrities)(
      (egraph, vbgVerifySample, xyzmoVerifyUser, order, product, celebrity) =>
        where(FilterOneTable.reduceFilters(filters, egraph) and celebrity.id === celebrityId)
          select(egraph, celebrity, vbgVerifySample, xyzmoVerifyUser)
          orderBy (egraph.id desc)
          on(egraph.id === vbgVerifySample.map(_.egraphId), egraph.id === xyzmoVerifyUser.map(_.egraphId),
          egraph.orderId === order.id, order.productId === product.id, product.celebrityId === celebrity.id)
    )
  }

  def findByOrder(orderId: Long, filters: FilterOneTable[Egraph]*): Query[Egraph] = {
    from(schema.egraphs)(egraph =>
      where(
        egraph.orderId === orderId and
          FilterOneTable.reduceFilters(filters, egraph)
      )
        select (egraph)
    )
  }

  //
  // SavesWithLongKey[Egraph] methods
  //
  override val table = schema.egraphs



  //
  // SavesCreatedUpdated[Egraph] methods
  //
  override def withCreatedUpdated(toUpdate: Egraph, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

class EgraphQueryFilters @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  import EgraphState._

  def pendingAdminReview: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState in Seq(PassedBiometrics.name, FailedBiometrics.name, ApprovedByAdmin.name))
      }
    }
  }

  def approvedByAdmin: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === ApprovedByAdmin.name)
      }
    }
  }

  def rejectedByAdmin: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === RejectedByAdmin.name)
      }
    }
  }

  def rejectedByMlb: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === RejectedByMlb.name)
      }
    }
  }

  def passedBiometrics: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === PassedBiometrics.name)
      }
    }
  }

  def failedBiometrics: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === FailedBiometrics.name)
      }
    }
  }

  def awaitingVerification: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === AwaitingVerification.name)
      }
    }
  }

  def pendingMlbReview: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === PendingMlbReview.name)
      }
    }
  }

  def published: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState === Published.name)
      }
    }
  }

  def publishedOrApproved: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        (egraph._egraphState in Seq(Published.name, ApprovedByAdmin.name))
      }
    }
  }

  def notRejected: FilterOneTable[Egraph] = {
    new FilterOneTable[Egraph] {
      override def test(egraph: Egraph) = {
        not(egraph._egraphState in Seq(RejectedByAdmin.name, RejectedByMlb.name))
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
