package models

import java.awt.image.BufferedImage
import services.{Dimensions, ImageUtil}

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
   * This doesn't belong here. It's the css class that identifies the frame
   * when it's being used for a product preview in the checkout flow.
   **/
  def previewCssClass: String

  /**
   * This doesn't belong here. It's the width at which the previews should be
   * rendered in the purchase flow.
   * This doesn't belong here.
   */
  def purchasePreviewWidth: Int

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

  /** Width of the image in pixels as displayed on the gallery page */
  def thumbnailWidthPixels: Int

  /** Height of the image in pixels as displayed on the gallery page **/
  def thumbnailHeightPixels: Int

  /** Width of image in pixels as display on gallery page when a pending order **/
  def pendingWidthPixels: Int

  /** Height of image in pixels as display on gallery page when a pending order **/
  def pendingHeightPixels: Int
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
   * @param image image to crop
   * @return a cropped copy of the image argument.
   */
  def cropImageForFrame(image: BufferedImage): BufferedImage = {
    val cropDimensions = getCropDimensions(image)
    ImageUtil.crop(image, cropDimensions)
  }

  /**
   * @param image image for which to calculate dimensions it would be cropped to
   * @return dimensions the image would be cropped to
   */
  def getCropDimensions(image: BufferedImage): Dimensions = {
    val targetAspectRatio = this.imageAspectRatio
    val originalWidth = image.getWidth.toDouble
    val originalHeight = image.getHeight.toDouble
    val originalAspectRatio = originalWidth / originalHeight

    if (originalAspectRatio < targetAspectRatio) {
      // the original is too tall. Use all of width and limit height.
      Dimensions(width = originalWidth.toInt, height = (originalWidth / targetAspectRatio).toInt)
    } else {
      // the original is too narrow. Use all of height and limit width.
      Dimensions(width = (originalHeight * targetAspectRatio).toInt, height = originalHeight.toInt)
    }
  }
}

object EgraphFrame {
  /**
   * Returns the suggested frame for a given image. Decision is made based on frame
   * dimensions
   */
  private def suggestedFrameForDimensions(dimensions: Dimensions): EgraphFrame = {
    if (dimensions.isLandscape) LandscapeEgraphFrame else PortraitEgraphFrame
  }

  /** See suggestedFrameForDimensions */
  def suggestedFrame(dimensions: Dimensions): EgraphFrame = {
    suggestedFrameForDimensions(dimensions)
  }
}

/**
 * The default egraph portrait frame
 * @deprecated With video-fied egraphs, we are dropping support for portrait-oriented egraphs.
 */
object PortraitEgraphFrame extends EgraphFrame {
  override val name: String = "Default Portrait"

  override val previewCssClass = "orientation-portrait"
  override val purchasePreviewWidth = 302

  override val cssClass  = "portrait"
  override val cssFrameColumnClasses = "offset1 span6"
  override val cssStoryColumnClasses = "span5"

  override val imageWidthPixels = 377
  override val imageHeightPixels = 526

  override val thumbnailWidthPixels = 350
  override val thumbnailHeightPixels = 525

  override val pendingWidthPixels = 170
  override val pendingHeightPixels = 225
}

/** The default egraph landscape photo frame */
object LandscapeEgraphFrame extends EgraphFrame {
  override val name = "Default Landscape"

  override val previewCssClass = "orientation-landscape"
  override val purchasePreviewWidth = 454

  override val cssClass  = "landscape"
  override val cssFrameColumnClasses = "span8"
  override val cssStoryColumnClasses = "span3"

  override val imageWidthPixels = 595
  override val imageHeightPixels = 377

  override val thumbnailWidthPixels = 510
  override val thumbnailHeightPixels = 410

  override val pendingWidthPixels = 230
  override val pendingHeightPixels = 185
}