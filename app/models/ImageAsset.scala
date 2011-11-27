package models

import models.ImageAsset.{Resolution, ImageType}
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import libs._
import java.awt.RenderingHints
import Blobs.Conversions._
import ImageUtil.Conversions._
import libs.Blobs.AccessPolicy

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
 * @param imageType the type of image: either ImageAsset.Png or ImageAsset.Jpeg
 *
 * @param resolution the resolution of this asset. This will either be MasterResolution, which can only be
 *   known by dereferencing masterData, or some CustomResolution(width, height).
 */
class ImageAsset(
  keyBase: String,
  name: String,
  masterData: => Array[Byte],
  imageType: ImageType,
  resolution: Resolution)
{
  import ImageAsset._

  /**
   * Persist this image asset to the blobstore
   */
  def save(access:AccessPolicy=AccessPolicy.Private): ImageAsset = {
    Blobs.put(
      key,
      renderFromMaster.asByteArray(imageType),
      access=access
    )

    this
  }

  /** Returns true that the image is accessible in the blobstore */
  def isPersisted: Boolean = {
    Blobs.exists(key)
  }

  /** Returns this ImageAsset transformed to the specified dimensions */
  def resized(width: Int, height: Int): ImageAsset = {
    new ImageAsset(keyBase, name, lazyMasterData, imageType, CustomResolution(width, height))
  }

  /** Attempts to fetch the asset from the blobstore. */
  def fetchImage: Option[BufferedImage] = {
    Blobs.get(key).map(theBlob => ImageIO.read(theBlob.asInputStream))
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
        ImageUtil.getScaledInstance(
          masterImage,
          width,
          height,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR,
          true // higher quality
        )
    }
  }

  /**
   * Returns the public url of the asset on the blobstore, assuming that the blob exists and is
   * publicly available.
   */
  def url: String = {
    Blobs.getUrl(key)
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

  private lazy val masterImage: BufferedImage = lazyMasterData.asBufferedImage
}

object ImageAsset {
  /**
   * Creates a new [[models.ImageAsset]] with [[models.ImageAsset.MasterResolution]] out of the provided data.
   * See [[models.ImageAsset]] constructor for description of these arguments.
   */
  def apply(masterData: => Array[Byte],
            keyBase: String,
            masterName: String,
            imageType: ImageType): ImageAsset =
  {
    new ImageAsset(keyBase, masterName, masterData, imageType, MasterResolution)
  }

  /**
   * Creates new [[models.ImageAsset]] with [[models.ImageAsset.MasterResolution]] that sources its master
   * data from the blobstore.
   */
  def apply(keyBase: String, masterName: String, imageType: ImageType): ImageAsset = {
    val masterKey = makeKey(keyBase, masterName, imageType)

    new ImageAsset(
      keyBase,
      masterName,
      Blobs.get(masterKey) match {
        case None => throw new IllegalStateException("Master data for image asset \""+masterKey+"\" unavailable in blobstore.")
        case Some(blob) => blob.asByteArray
      },
      imageType,
      MasterResolution
    )
  }


  /**
   * Constructs a blobstore key for a [[models.ImageAsset]] out of the constituent parts.
   * See constructor for [[models.ImageAsset]] for detailed descriptions of these arguments.
   */
  def makeKey(keyBase: String,
              masterName: String,
              imageType: ImageType,
              resolution:Resolution=MasterResolution): String =
  {
    val resolutionPhrase = resolution match {
      case MasterResolution =>
        "master"

      case CustomResolution(width, height) =>
        width + "x" + height
    }

    keyBase + "/" + masterName + "/" + resolutionPhrase + "." + imageType.extension
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

  /** Supported image formats for an [[models.ImageAsset]] */
  sealed abstract class ImageType(val extension: String)
  case object Png extends ImageType("png")
  case object Jpeg extends ImageType("jpg")
}