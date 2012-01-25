package models

import models.ImageAsset.{Resolution, ImageType}
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import libs._
import java.awt.RenderingHints
import Blobs.Conversions._
import ImageUtil.Conversions._
import libs.Blobs.AccessPolicy
import com.google.inject.Inject
import services.AppConfig

case class ImageAssetServices @Inject() (blobs: Blobs, images: ImageUtil)

/**
 * A resizable image that gets persisted in the blob store. All transforms are created by
 * resizing the master image whose data must be available via the masterData constructor argument.
 *
 * Favor the companion object apply methods for constructing instances.
 *
 * @param keyBase the "folder" in the blobstore in which the image and its transforms will be placed.
 *   for example, "egraphs/1234" if we'll be generating images for the Egraph with ID 1234
 *
 * @param name the asset's name. For example, "profile".
 *   Combined with the imageType parameter, this will determine the blobstore key of the image and its
 *   permutations. For example, paired with ImageAsset.Png the key could be "egraph/1234/profile/master.png",
 *   for the master, or "egraph/1234/profile/100x100.png" for the 100x100 permutation of the master.
 *
 * @param masterData a statement that, when called, will produce the master image's bytes. As this may
 *   involve a round-trip to the blobstore, this is guaranteed to only be called once for the entire
 *   permutation graph of an ImageAsset in memory. It is only called at the moment the bytes of data are
 *   needed.
 *
 * @param imageType the type of image: either [[models.ImageAsset.Png]] or [[models.ImageAsset.Jpeg]].
 *   It is highly recommended to make your master image a `png` as the format is lossless.
 *
 * @param resolution the resolution of this asset. This will either be MasterResolution, which can only be
 *   known by dereferencing masterData, or a custom resolution.
 */
class ImageAsset(
  keyBase: String,
  name: String,
  masterData: => Array[Byte],
  imageType: ImageType,
  resolution: Resolution,
  services: ImageAssetServices)
{
  import ImageAsset._

  /**
   * Persist this image asset to the blobstore
   */
  def save(access:AccessPolicy=AccessPolicy.Private): ImageAsset = {
    services.blobs.put(
      key,
      renderFromMaster.asByteArray(imageType),
      access=access
    )

    this
  }

  /**
   * Ensures that configured ImageAsset is available on the blobstore by querying
   * the blobstore and rendering/saving the data if it was not present.
   *
   * @return the saved ImageAsset
   */
  def getSaved(access:AccessPolicy=AccessPolicy.Private): ImageAsset = {
    if (!isPersisted) {
      save(access)
    }
    else {
      this
    }
  }

  /** Returns true that the image is accessible in the blobstore */
  def isPersisted: Boolean = {
    services.blobs.exists(key)
  }

  /** Returns this ImageAsset transformed to the specified resolution */
  def resized(width: Int, height: Int): ImageAsset = {
    withResolution(CustomResolution(width, height))
  }

  def withImageType(newImageType: ImageType): ImageAsset = {
    new ImageAsset(keyBase, name, lazyMasterData, newImageType, resolution, services)
  }

  /** Proportionally resizes the width of the image */
  def resizedWidth(width: Int): ImageAsset = {
    withResolution(CustomWidthResolution(width))
  }

  /** Proportionally resizes the height of the image */
  def resizedHeight(height: Int): ImageAsset = {
    withResolution(CustomHeightResolution(height))
  }

  /** Attempts to fetch the asset from the blobstore. */
  def fetchImage: Option[BufferedImage] = {
    services.blobs.get(key).map(theBlob => ImageIO.read(theBlob.asInputStream))
  }

  /**
   * Renders the asset from the provided master data.
   *
   * Throws runtime exceptions if the data were unavailable for
   * whatever reason.
   */
  def renderFromMaster: BufferedImage = {
    resolution match {
      case MasterResolution =>
        masterImage

      case CustomResolution(width, height) =>
        resizedMasterImage(width, height)

      case CustomWidthResolution(width) =>
        val scaleFactor = width.toDouble / masterImage.getWidth.toDouble
        resizedMasterImage(width, (masterImage.getHeight * scaleFactor).toInt)

      case CustomHeightResolution(height) =>
        val scaleFactor = height.toDouble / masterImage.getHeight.toDouble
        resizedMasterImage((masterImage.getHeight * scaleFactor).toInt, height)
    }
  }

  /**
   * Returns the public url of the asset on the blobstore, assuming that the blob exists and is
   * publicly available.
   */
  def url: String = {
    services.blobs.getUrl(key)
  }

  /**
   * Returns the public url of the asset on the blobstore, or None if the asset doesn't exist.
   *
   * This may be expensive as it involves a round trip to the blobstore.
   */
  def urlOption: Option[String] = {
    if (isPersisted) Some(url) else None
  }

  /**
   * The blobstore key of the asset, if it exists.
   */
  lazy val key: String = {
    makeKey(keyBase, name, imageType, resolution)
  }

  //
  // Private members
  //
  /**
   * A lazy val that dereferences masterData. We make it lazy val so we only ever grab it once,
   * as it may be an expensive operation. By passing this member as the masterData constructor
   * field of all ImageAssets derived from this one, we ensure that it only ever gets
   * evaluated once.
   */
  private lazy val lazyMasterData: Array[Byte] = masterData

  /** Renders the master data as an image */
  private lazy val masterImage: BufferedImage = lazyMasterData.asBufferedImage

  /** Returns a copy of this ImageAsset with the new Resolution */
  private def withResolution(newResolution: Resolution): ImageAsset = {
    new ImageAsset(keyBase, name, lazyMasterData, imageType, newResolution, services)
  }

  /** Returns the master data scaled to the specified width and height */
  private def resizedMasterImage(width: Int,  height: Int): BufferedImage = {
    services.images.getScaledInstance(masterImage, width, height, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
  }
}

object ImageAsset {
  /**
   * Creates a new [[models.ImageAsset]] with [[models.ImageAsset.MasterResolution]] out of the provided data.
   * See [[models.ImageAsset]] constructor for description of these arguments.
   */
  def apply(masterData: => Array[Byte],
            keyBase: String,
            name: String,
            imageType: ImageType,
            services:ImageAssetServices): ImageAsset =
  {
    new ImageAsset(keyBase, name, masterData, imageType, MasterResolution, services=services)
  }

  /**
   * Creates new [[models.ImageAsset]] with [[models.ImageAsset.MasterResolution]] that sources its master
   * data from the blobstore.
   */
  def apply(keyBase: String, name: String, imageType: ImageType, services:ImageAssetServices): ImageAsset = {
    val masterKey = makeKey(keyBase, name, imageType)

    new ImageAsset(
      keyBase,
      name,
      services.blobs.get(masterKey) match {
        case None => throw new IllegalStateException("Master data located at \""+masterKey+"\" unavailable in blobstore.")
        case Some(blob) => blob.asByteArray
      },
      imageType,
      MasterResolution,
      services=services
    )
  }

  /**
   * Constructs a blobstore key for a [[models.ImageAsset]] out of the constituent parts.
   * See constructor for [[models.ImageAsset]] for detailed descriptions of these arguments.
   */
  def makeKey(keyBase: String,
              name: String,
              imageType: ImageType,
              resolution:Resolution=MasterResolution): String =
  {
    val resolutionPhrase = resolution match {
      case MasterResolution =>
        "master"

      case CustomResolution(width, height) =>
        width + "x" + height

      case CustomWidthResolution(width) =>
        "w" + width

      case CustomHeightResolution(height) =>
        "h" + height
    }

    keyBase + "/" + name + "/" + resolutionPhrase + "." + imageType.extension
  }

  /** Represents the pixel resolution of an [[models.ImageAsset]]. */
  sealed trait Resolution

  /**
   * Resolution of the original image source. It can not be known in pixels
   * without accessing the master data itself.
   */
  case object MasterResolution extends Resolution

  /** A resolution specified in pixels */
  case class CustomResolution(width: Int, height: Int) extends Resolution

  /**
   * A resolution with specified width. When the transform actually occurs the height is calculated
   * against the master image to maintain the correct photo aspect ratio
   */
  case class CustomWidthResolution(width: Int) extends Resolution

  /**
   * A resolution with specified height. When the transform actually occurs the width is calculated
   * against the master image to maintain the correct photo aspect ratio.
   */
  case class CustomHeightResolution(height: Int) extends Resolution

  /** Supported image formats for an [[models.ImageAsset]] */
  sealed abstract class ImageType(val extension: String)
  case object Png extends ImageType("png")
  case object Jpeg extends ImageType("jpg")
}