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
 * A resizable image that gets persisted in the blob store.
 */
class ImageAsset(
  keyBase: String,
  masterName: String,
  masterData: => Array[Byte],
  imageType: ImageType,
  resolution: Resolution)
{
  import ImageAsset._

  def save(access:AccessPolicy=AccessPolicy.Private) {
    Blobs.put(
      key,
      renderFromMaster.asByteArray(imageType),
      access=access
    )
  }

  def isPersisted: Boolean = {
    Blobs.exists(key)
  }

  def resized(newResolution: Resolution): ImageAsset = {
    new ImageAsset(keyBase, masterName, lazyMasterData, imageType, newResolution)
  }

  def fetchImageOption: Option[BufferedImage] = {
    Blobs.get(key).map(theBlob => ImageIO.read(theBlob.asInputStream))
  }

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

  def url: String = {
    Blobs.getUrl(key)
  }

  def urlOption: Option[String] = {
    if (isPersisted) Some(url) else None
  }

  //
  // Private members
  //
  /**
   * Keep a lazy val for the master data option so that we only ever grab it once, as it
   * may be an expensive operation. By passing this member as the masterData constructor
   * field of all ImageAssets derived from this one, we ensure that it only ever gets
   * evaluated once.
   */
  private lazy val lazyMasterData: Array[Byte] = masterData

  private lazy val masterImage: BufferedImage = lazyMasterData.asBufferedImage

  private lazy val key: String = {
    makeKey(keyBase, masterName, imageType, resolution)
  }

}

object ImageAsset {
  def apply(masterData:Array[Byte], keyBase: String, masterName: String, imageType: ImageType): ImageAsset = {
    new ImageAsset(keyBase, masterName, masterData, imageType, MasterResolution)
  }

  def apply(keyBase: String, masterName: String, imageType: ImageType): ImageAsset = {
    val masterKey = makeKey(keyBase, masterName, imageType)

    new ImageAsset(
      keyBase,
      masterName,
      Blobs.get(masterKey) match {
        case None => throw new IllegalStateException("Master data for image asset \""+masterKey+"\" unavailable.")
        case Some(blob) => blob.asByteArray
      },
      imageType,
      MasterResolution
    )
  }

  def makeKey(keyBase: String, masterName: String, imageType: ImageType, resolution:Resolution=MasterResolution): String = {
    val resolutionPhrase = resolution match {
      case MasterResolution =>
        ""

      case CustomResolution(width, height) =>
        "_" + width + "x" + height
    }

    keyBase + "/" + masterName + resolutionPhrase + "." + imageType.extension
  }

  sealed abstract class Resolution(val width: Int, val height: Int)
  
  case object MasterResolution extends Resolution(-1, -1)
  case class CustomResolution(override val width:Int, override val height:Int) extends Resolution(width, height)

  sealed abstract class ImageType(val extension: String)
  case object Png extends ImageType("png")
  case object Jpeg extends ImageType("jpg")
}