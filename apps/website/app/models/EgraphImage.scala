package models

import com.google.inject.Inject
import java.awt.image.BufferedImage
import java.awt.geom.AffineTransform
import services.blobs.{AccessPolicy, Blobs}
import services.logging.Logging
import services.{Time, Dimensions, ImageUtil, AppConfig}
import services.graphics._
import java.text.DecimalFormat

/**
 * The visual representation of an Egraph.
 *
 * Provides means of transforming the image and optionally rendering or retrieving it
 * from the blobstore.
 *
 * @param ingredientFactory function that returns the data necessary to render the image.
 *     The function will only be called when it becomes necessary to render the image.
 * @param graphicsSource Source of the canvas upon which to draw the image.
 * @param blobPath Root blobstore path where permutations of the image should be stored. For example,
 *     the first Egraph will probably pass usually be "egraphs/1/image"
 * @param transforms Series of transforms that should be performed on the "default" Egraph image
 *     before rendering it. The "default" image is the egraph at exactly the size and shape
 *     it was signed on Celebrity's tablet.
 * @param services The functionality necessary to manipulate the EgraphImage's data.
 */
case class EgraphImage (
  ingredientFactory: () => EgraphImageIngredients,
  graphicsSource: GraphicsSource,
  blobPath: String,
  transforms: Vector[EgraphImageTransform] = Vector.empty,
  services: EgraphImageServices = AppConfig.instance[EgraphImageServices]
) {
  import EgraphImage._

  /** Proportionally scales the image to the specified width in pixels */
  def scaledToWidth(newWidth: Int): EgraphImage = {
    withTransform(ScaleToWidthEgraphImageTransform(newWidth))
  }

  /** @see ChangePenWidthEgraphImageTransform */
  def withPenWidth(newWidth: Double): EgraphImage = {
    withTransform(ChangePenWidthEgraphImageTransform(newWidth))
  }

  /** @see ChangeShadowOffsetEgraphImageTransform */
  def withPenShadowOffset(offsetX: Double, offsetY: Double): EgraphImage = {
    withTransform(ChangeShadowOffsetEgraphImageTransform(offsetX, offsetY))
  }

  /** @see SigningOriginOffsetEgraphImageTransform */
  def withSigningOriginOffset(signingOriginX: Double, signingOriginY: Double): EgraphImage = {
    withTransform(SigningOriginOffsetEgraphImageTransform(signingOriginX, signingOriginY))
  }

  /**
   * Returns a version of this EgraphImage that will render as a rasterized PNG rather than
   * the default vector
   **/
  def asPng: EgraphImage = {
    this.copy(graphicsSource = services.rasterGraphicsSourceFactory())
  }

  /** Returns a copy of this image with the supplied transform applied */
  private def withTransform(transform: EgraphImageTransform): EgraphImage = {
    this.copy(transforms = transforms :+ transform)
  }

  /**
   * Attempts to get the URL of the image with the receiver's specified transforms
   * from the blobstore.
   *
   * If the blobstore does not currently contain the image (or overwrite is true), it renders the
   * image, saves it to the blobstore, and returns the URL.
   **/
  def getSavedUrl(accessPolicy: AccessPolicy, overwrite:Boolean=false): String = {
    val blobs = services.blobs

    val ((url, alreadyCached), durationSecs) = Time.stopwatch {
      blobs.getUrlOption(blobKey) match {
        case Some(alreadySavedUrl) => (alreadySavedUrl, true)
        case None => (saveAndGetUrl(accessPolicy), false)
      }
    }
    if (alreadyCached) {
      log("Retrieving cached EgraphImage with key \"" + blobKey + "\" in " + durationSecs + "s")
    } else {
      log("Rendering and returning EgraphImage with key \"" + blobKey + "\" in " + durationSecs + "s")
    }

    url
  }


  /**
   * Renders the image, writes it to the blobstore, and returns its URL.
   */
  def saveAndGetUrl(accessPolicy: AccessPolicy): String = {
    log("Rendering EgraphImage with key \"" + blobKey + "\"")
    val (url, durationSecs) = Time.stopwatch {
      val rendered = transformAndRender
      val bytes = rendered.graphicsSource.asByteArray
      val blobs = services.blobs
      blobs.put(blobKey, bytes, accessPolicy)
      blobs.getUrlOption(blobKey).get
    }
    log("Rendered the EgraphImage in " + durationSecs + "s")

    url
  }

  /**
   * Performs all the specified transforms on the image, then renders the result.
   * This is possibly a very expensive operation as it will reach out to the DB and
   * blobstore to grab the image JSON and product photo.
   *
   * @return the transformed and rendered EgraphImage
   */
  /*private*/ def transformAndRender: EgraphImage = {
    // Perform the transforms
    val transformedImage = transforms.foldLeft(this) { (currentImage, nextTransform) =>
      nextTransform(currentImage)
    }

    // Render the ingredients. This could be expensive because gathering the ingredients
    // is lazy, and could have deferred a GET from S3 for the product photo.
    val ingredients = transformedImage.ingredientFactory()
    val photo = ingredients.photo
    val allHandwriting = ingredients.allHandwriting
    val pen = ingredients.pen
    val newGraphicsSource = graphicsSource.withDimensions(photo.getWidth, photo.getHeight)

    // Get the graphics and draw the image without mucking up any pixels.
    val graphics = newGraphicsSource.graphics

    //   BTW you may think it makes more sense to use drawImage(BufferedImage, AffineTransformOp).
    //   You are wrong. Do that and all your JPEGs will show up in technicolor.
    graphics.drawRenderedImage(photo, new AffineTransform())

    // Write on top of the image
    pen.write(allHandwriting, graphics)

    // Return a copy of the image with the rendered graphics source
    transformedImage.copy(graphicsSource=newGraphicsSource)
  }

  /**
   * The key by which this EgraphImage will be known in the blobstore. It is a composite
   * of the set of transforms applied to the image.
   **/
  private val blobKey: String = {
    val idString = transforms match {
      // Call it "default" if it's the default implementation
      case Vector() =>
        "default"

      case _ =>
        val ids = transforms.map(theTransform => theTransform.id)
        ids.mkString(sep="_")
    }

    blobPath + "/" + idString + "-v" + EgraphImage.Version + "." + graphicsSource.fileExtension
  }
}


object EgraphImage extends Logging {
  // Increment this every time we improve EgraphImage rendering to avoid
  // Caches serving up old versions.
  val Version = 1
}

/**
 * Services necessary for EgraphImage to perform its full functionality.
 *
 * @param blobs the application blobstore
 */
case class EgraphImageServices @Inject() (
  blobs: Blobs,
  rasterGraphicsSourceFactory: () => RasterGraphicsSource
)

/**
 * Data necessary for EgraphImage to perform its full functionality.
 *
 * It is separated into its own class to make it easier to lazily evaluate it at
 * the moment of rendering.
 *
 * @param signature the Handwriting derived from the signature's JSON vectors.
 * @param message the Handwriting derived from the message's JSON vectors
 * @param pen the pen that should be used to draw handwriting above the EgraphImage
 * @param photo The photo that should be signed
 * @param photoDimensionsWhenSigned The dimensions of the photo when it was originally signed. This is
 *     important to track because you can not proportionally scale the drawing algorithm without knowing
 *     this piece of data.
 */
case class EgraphImageIngredients(
  signature: Handwriting,
  message: Option[Handwriting],
  pen: HandwritingPen,
  photo: BufferedImage,
  photoDimensionsWhenSigned: Dimensions,
  signingOriginX: Int,
  signingOriginY: Int
) {
  /**
   * Returns the combination of signature and message Handwriting.
   */
  def allHandwriting: Handwriting = {
    message.map(messageWriting => messageWriting.append(signature)).getOrElse(signature)
  }
}


object EgraphImageIngredients {
  /**
   * Convenience to create EgraphImageIngredients when you have only JSON strings instead of
   * Handwriting objects for the signature and message.
   */
  def apply(
    signatureJson: String,
    messageJsonOption: Option[String],
    pen: HandwritingPen,
    photo: BufferedImage,
    photoDimensionsWhenSigned: Dimensions,
    signingOriginX: Int,
    signingOriginY: Int
  ): EgraphImageIngredients = {
    val signatureHandwriting = Handwriting(signatureJson)
    val messageHandwritingOption = messageJsonOption.map { messageJson =>
      Handwriting(messageJson).append(signatureHandwriting)
    }

    EgraphImageIngredients(
      signatureHandwriting,
      messageHandwritingOption,
      pen,
      photo,
      photoDimensionsWhenSigned,
      signingOriginX,
      signingOriginY
    )
  }
}


/**
 * Describes the behavior of any transformation that can be made to an EgraphImage
 */
trait EgraphImageTransform {
  /**
   * Uniquely identifies the transform that occurred. For example, if the transform intended
   * to scale the handwriting by 2 in size its 'id' might be "handwriting-x-2".
   *
   * The returned string should only contain characters that are safe to use in a blobstore key
   *
   */
  def id: String

  /**
   * Returns a copy of the image with the transform applied.
   *
   * @param egraphImage the image to transform
   */
  def apply(egraphImage: EgraphImage): EgraphImage
}

/**
 * Proportionally scales the entire EgraphImage to a specified pixel width. This should generally
 * be used when *downscaling* the image.
 *
 * @param newWidth the new desired pixel width.
 */
case class ScaleToWidthEgraphImageTransform(newWidth: Int) extends EgraphImageTransform {
  import ScaleToWidthEgraphImageTransform._

  override val id = "global-width-" + newWidth + "px"

  def apply(toTransform: EgraphImage): EgraphImage = {
    log("Applying transform")
    // Grab the ingredients of the egraph image. This could be expensive as it may have to go to the database and S3.
    val previousIngredients = toTransform.ingredientFactory()
    val previousPhoto = previousIngredients.photo
    val previousPen = previousIngredients.pen
    val previousSignature = previousIngredients.signature
    val previousMessageOption = previousIngredients.message
    val signedPhotoDimensions = previousIngredients.photoDimensionsWhenSigned

    // Calculate scale factors
    val photoScaleFactor = newWidth.toDouble / previousPhoto.getWidth.toDouble
    val handwritingScaleFactor = (newWidth.toDouble / signedPhotoDimensions.width.toDouble)

    // Scale photo if necessary
    val newPhotoHeight = (previousPhoto.getHeight * photoScaleFactor).toInt
    val newGraphicsSource = toTransform.graphicsSource.withDimensions(newWidth, newPhotoHeight)

    val newPhoto = if ((newWidth, newPhotoHeight) == (previousPhoto.getWidth, previousPhoto.getHeight)) {
       previousPhoto
    } else {
      ImageUtil.getScaledInstance(previousPhoto, newWidth, newPhotoHeight)
    }

    // Scale handwriting and pen
    log("Handwriting scales by " + handwritingScaleFactor)
    val newPen = previousPen.scalingStrokeBy(handwritingScaleFactor)
    val newSignature = previousSignature.scalingBy(handwritingScaleFactor)
    val newMessageOption = previousMessageOption.map(message => message.scalingBy(handwritingScaleFactor))

    // Create new ingredients and return an EgraphImage that uses them.
    val newIngredients = previousIngredients.copy(
      photo=newPhoto,
      pen=newPen,
      signature=newSignature,
      message=newMessageOption
    )

    toTransform.copy(ingredientFactory= ()=> newIngredients, graphicsSource=newGraphicsSource)
  }
}


object ScaleToWidthEgraphImageTransform extends Logging


/**
 * Transform that alters the pen-width absolutely in the pixels defined by the coordinate space
 * at time of signing
 **/
case class ChangePenWidthEgraphImageTransform(width: Double) extends EgraphImageTransform {
  override val id = "pen-width-" + new DecimalFormat("0.00").format(width)

  override def apply(toTransform: EgraphImage): EgraphImage = {
    val oldIngredients = toTransform.ingredientFactory()
    val newPen = oldIngredients.pen.copy(width=width)
    toTransform.copy(ingredientFactory= ()=> oldIngredients.copy(pen=newPen))
  }
}

/**
 * Transform that alters the offset of the shadow from the pen absolutely in the pixels defined by
 * the coordinate space at time of signing.
 */
case class ChangeShadowOffsetEgraphImageTransform(offsetX: Double, offsetY: Double) extends EgraphImageTransform {
  override val id = {
    val numberFormat = new DecimalFormat("0.00")

    "shadow-offset-" + numberFormat.format(offsetX) + "x" + numberFormat.format(offsetY)
  }

  override def apply(toTransform: EgraphImage): EgraphImage = {
    val oldIngredients = toTransform.ingredientFactory()
    val oldPen = oldIngredients.pen
    val newShadowOption = oldPen.shadowOption.map { shadow => shadow.copy(offsetX, offsetY) }
    val newPen = oldPen.copy(shadowOption=newShadowOption)

    toTransform.copy(ingredientFactory= ()=> oldIngredients.copy(pen=newPen))
  }
}

case class SigningOriginOffsetEgraphImageTransform(signingOriginX: Double, signingOriginY: Double) extends EgraphImageTransform {
  override val id = {
    val numberFormat = new DecimalFormat("0")

    "signing-origin-offset-" + numberFormat.format(signingOriginX) + "x" + numberFormat.format(signingOriginY)
  }

  override def apply(toTransform: EgraphImage): EgraphImage = {
    val oldIngredients = toTransform.ingredientFactory()
    val previousSignature = oldIngredients.signature
    val previousMessageOption = oldIngredients.message
    val newSignature = previousSignature.translatingBy(signingOriginX, signingOriginY)
    val newMessageOption = previousMessageOption.map(message => message.translatingBy(signingOriginX, signingOriginY))

    toTransform.copy(ingredientFactory= ()=> oldIngredients.copy(signature=newSignature,message=newMessageOption))
  }
}
