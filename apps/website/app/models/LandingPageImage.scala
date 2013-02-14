package models

<<<<<<< HEAD
import services.{Dimensions, ImageUtil, Time}
=======
import services.{ImageUtil, Time}
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
import scala.Some
import java.awt.image.BufferedImage

/**
 * Provides some default functionality for objects that want to have a 1550x556px image for shoving in the masthead
 * of the landing page and around some other spots like the marketplace.
 * @tparam T
 */

trait LandingPageImage[T] {
  def _landingPageImageKey: Option[String]
  def defaultLandingPageImage : ImageAsset
  def keyBase: String
  def withLandingPageImageKey(key: Option[String]) : T with LandingPageImage[T]
  def imageAssetServices: ImageAssetServices
  def save: T


  def withLandingPageImage(imageData: Array[Byte]): (T with LandingPageImage[T], ImageAsset) = {
    val newImageKey = "landing_" + Time.toBlobstoreFormat(Time.now)

    val entity = withLandingPageImageKey(key = Some(newImageKey))
    val image = ImageAsset(imageData, keyBase, newImageKey, ImageAsset.Png, imageAssetServices)
    (entity, image)
  }

  def saveWithLandingPageImage(landingPageImage: Option[BufferedImage]): T = {
    landingPageImage match {
      case None => this.asInstanceOf[T]
      case Some(image) => {
        val landingPageImageBytes = {
          import services.ImageUtil.Conversions._
          // Crops that ish so it doesn't mess with the page layouts.
<<<<<<< HEAD
          val croppedImage = ImageUtil.crop(image, LandingPageImage.defaultLandingPageImageDimensions)
=======
          val croppedImage = ImageUtil.crop(image, Celebrity.defaultLandingPageImageDimensions)
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
          croppedImage.asByteArray(ImageAsset.Jpeg)
        }
        val (entity, newImage) = withLandingPageImage(landingPageImageBytes)
        newImage.save()
        entity.save
      }
    }
  }

  def landingPageImage: ImageAsset = {
    _landingPageImageKey.flatMap(theKey => Some(ImageAsset(keyBase, theKey, ImageAsset.Png, services=imageAssetServices))) match {
      case Some(imageAsset) => imageAsset
      case None => defaultLandingPageImage
    }
  }
}
<<<<<<< HEAD

object LandingPageImage {
  val minImageWidth = 1550
  val minImageHeight = 556
  val defaultLandingPageImageDimensions = Dimensions(width = minImageWidth, height = minImageHeight)
  val landingPageImageAspectRatio = minImageWidth.toDouble / minImageHeight
}
=======
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
