package models

import utils.{ClearsDatabaseAndValidationBefore, DBTransactionPerTest, EgraphsUnitTest}
import java.io.FileOutputStream
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import play.Play
import services.{ImageUtil, TempFile, AppConfig}
import services.blobs.AccessPolicy.Private

class ImageAssetTests extends EgraphsUnitTest
  with DBTransactionPerTest
  with ClearsDatabaseAndValidationBefore
{
  val assetServices = AppConfig.instance[ImageAssetServices]

  import ImageUtil.Conversions._

  val keyBase = "egraph/1234"
  val assetName = "profile"

  "An ImageAsset" should "have the correct master data" in {
    val image = imageFromDisk
    val imageBytes = image.asByteArray(ImageAsset.Png)

    val asset = ImageAsset(imageBytes, keyBase, assetName, ImageAsset.Png, assetServices)

    new FileOutputStream(TempFile.named("img_orig.png")).write(imageBytes)
    ImageIO.write(asset.renderFromMaster, "png", TempFile.named("img_rendered.png"))

    asset.renderFromMaster.asByteArray(ImageAsset.Png).toSeq.length should be (imageBytes.toSeq.length)
  }

  it should "have the right key, both for master and permutations" in {
    val asset = ImageAsset(Array.empty[Byte], keyBase, assetName, ImageAsset.Png, assetServices)

    asset.key should be ("egraph/1234/profile/master.png")
    asset.resized(100, 100).key should be ("egraph/1234/profile/100x100.png")
  }

  it should "only dereference the master data once when called from transforms first" in {
    var dereferenceCount = 0
    val asset = makeAsset({dereferenceCount += 1; imageFromDisk.asByteArray(ImageAsset.Png)})
    dereferenceCount should be (0)

    val asset_100x100 = asset.resized(100, 100)
    dereferenceCount should be (0)

    val asset_50x50 = asset_100x100.resized(50, 50)
    dereferenceCount should be (0)

    asset_50x50.renderFromMaster
    dereferenceCount should be (1)

    asset_100x100.renderFromMaster
    asset.renderFromMaster
    dereferenceCount should be (1)
  }

  it should "only dereference the master data once when called from master first" in {
    // No matter how many transforms we do we should only grab the master data from source once.
    var dereferenceCount = 0
    val asset = makeAsset({dereferenceCount += 1; imageFromDisk.asByteArray(ImageAsset.Png)})

    asset.renderFromMaster
    dereferenceCount should be (1)

    asset.resized(100, 100).renderFromMaster
    asset.resized(50, 50).renderFromMaster
    asset.resized(50, 50).resized(25, 25).renderFromMaster
    dereferenceCount should be (1)
  }

  it should "generate a properly resized image" in {
    val asset_100x100 = makeAsset(imageFromDisk.asByteArray(ImageAsset.Png)).resized(100, 100)
    val image = asset_100x100.renderFromMaster

    (image.getWidth, image.getHeight) should be ((100, 100))
  }

  it should "properly save and restore the master copy" in {
    val asset = makeAsset(imageFromDisk.asByteArray(ImageAsset.Png))
    val assetPngData = asset.renderFromMaster.asByteArray(ImageAsset.Png)

    asset.fetchImage should be (None)
    asset.isPersisted should be (false)

    asset.save()

    asset.isPersisted should be (true)
    asset.fetchImage.get.asByteArray(ImageAsset.Png) should be (assetPngData)
  }

  it should "source from a stored master copy in blobstore when instantiated without data from companion object" in {
    val storedAsset = makeAsset(imageFromDisk.asByteArray(ImageAsset.Png))
    val storedBytes = storedAsset.renderFromMaster.asByteArray(ImageAsset.Png)
    storedAsset.save()

    val restoredAsset = ImageAsset(keyBase, assetName, ImageAsset.Png, assetServices)
    val restoredBytes = restoredAsset.renderFromMaster.asByteArray(ImageAsset.Png)

    restoredBytes.toSeq should be (storedBytes.toSeq)
  }

  it should "throw an IllegalStateException when trying to source from a master blob that doesn't exist" in {
    val restoredAsset = ImageAsset(keyBase, assetName, ImageAsset.Png, assetServices)
    evaluating { restoredAsset.renderFromMaster } should produce [IllegalStateException]
  }

  it should "correctly store and fetch permutations as {name}/{width}x{height}" in {
    val asset = makeAsset(imageFromDisk.asByteArray(ImageAsset.Png))

    val resized = asset.resized(100, 100)
    val resizedBytes = resized.renderFromMaster.asByteArray(ImageAsset.Png)

    resized.fetchImage should be (None)

    resized.save()
    
    resized.fetchImage.get.asByteArray(ImageAsset.Png).toSeq should be (resizedBytes.toSeq)
  }

  it should "return the correct url" in {
    val asset = makeAsset(imageFromDisk.asByteArray(ImageAsset.Png))
    asset.save(Private)
    asset.url should include (asset.key)
  }

  it should "only provide a urlOption if the data are available" in {
    val asset = makeAsset(imageFromDisk.asByteArray(ImageAsset.Png))

    asset.urlOption should be (None)

    asset.save().urlOption should be (Some(asset.url))
  }
    
  def imageFromDisk: BufferedImage = {
    ImageIO.read(Play.getFile("test/files/image.png"))
  }

  def makeAsset(bytes: => Array[Byte]) = {
    ImageAsset(bytes, keyBase, assetName, ImageAsset.Png, assetServices)
  }
}