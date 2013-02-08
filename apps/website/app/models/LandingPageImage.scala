package models

import services.Time
import scala.Some

trait LandingPageImage[T] {
  def _landingPageImageKey: Option[String]
  def defaultLandingPageImage : ImageAsset
  def keyBase: String
  def withLandingPageImageKey(key: Option[String]) : T
  def imageAssetServices: ImageAssetServices


  def withLandingPageImage(imageData: Array[Byte]): (T, ImageAsset) = {
    val newImageKey = "landing_" + Time.toBlobstoreFormat(Time.now)

    val entity = withLandingPageImageKey(key = Some(newImageKey))
    val image = ImageAsset(imageData, keyBase, newImageKey, ImageAsset.Png, imageAssetServices)
    (entity, image)
  }

  def landingPageImage: ImageAsset = {
    _landingPageImageKey.flatMap(theKey => Some(ImageAsset(keyBase, theKey, ImageAsset.Png, services=imageAssetServices))) match {
      case Some(imageAsset) => imageAsset
      case None => defaultLandingPageImage
    }
  }
}
