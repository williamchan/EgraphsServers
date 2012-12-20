package models

import utils.{ClearsCacheBefore, DBTransactionPerTest, EgraphsUnitTest}
import java.io.FileOutputStream
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import play.api.Play
import services.{ImageUtil, TempFile, AppConfig}
import services.blobs.AccessPolicy.Private
import org.apache.commons.lang3.RandomStringUtils

class ImageAssetTests extends EgraphsUnitTest
  with DBTransactionPerTest
  with ClearsCacheBefore
{
  private def assetServices = AppConfig.instance[ImageAssetServices]

  import ImageUtil.Conversions._

  val assetName = "profile"

  lazy val imageBytes: Array[Byte] = {
    val image = ImageIO.read(EgraphsUnitTest.resourceFile("image.jpg"))
    image.asByteArray(ImageAsset.Png)
  }

  "An ImageAsset" should "have the correct master data" in new EgraphsTestApplication {
    val asset = newImageAsset()

    new FileOutputStream(TempFile.named("img_orig.png")).write(imageBytes)
    ImageIO.write(asset.renderFromMaster, "png", TempFile.named("img_rendered.png"))

    asset.renderFromMaster.asByteArray(ImageAsset.Png).toSeq.length should be (imageBytes.toSeq.length)
  }

  it should "have the right key, both for master and permutations" in new EgraphsTestApplication {
    val (asset, keyBase) = newImageAssetAndKeyBase(Array.empty[Byte])

    asset.key should be (keyBase + "/profile/master.png")
    asset.resized(100, 100).key should be (keyBase + "/profile/100x100.png")
  }

  it should "only dereference the master data once when called from transforms first" in new EgraphsTestApplication {
    var dereferenceCount = 0
    val asset = newImageAsset({dereferenceCount += 1; imageBytes})
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

  it should "only dereference the master data once when called from master first" in new EgraphsTestApplication {
    // No matter how many transforms we do we should only grab the master data from source once.
    var dereferenceCount = 0
    val asset = newImageAsset({dereferenceCount += 1; imageBytes})

    asset.renderFromMaster
    dereferenceCount should be (1)

    asset.resized(100, 100).renderFromMaster
    asset.resized(50, 50).renderFromMaster
    asset.resized(50, 50).resized(25, 25).renderFromMaster
    dereferenceCount should be (1)
  }

  it should "generate a properly resized image" in new EgraphsTestApplication {
    val asset_100x100 = newImageAsset().resized(100, 100)
    val image = asset_100x100.renderFromMaster

    (image.getWidth, image.getHeight) should be ((100, 100))
  }

  it should "properly save and restore the master copy" in new EgraphsTestApplication {
    val asset = newImageAsset()
    val assetPngData = asset.renderFromMaster.asByteArray(ImageAsset.Png)

    asset.fetchImage should be (None)
    asset.isPersisted should be (false)

    asset.save()

    asset.isPersisted should be (true)
    asset.fetchImage.get.asByteArray(ImageAsset.Png) should be (assetPngData)
  }

  it should "source from a stored master copy in blobstore when instantiated without data from companion object" in new EgraphsTestApplication {
    val (storedAsset, keyBase) = newImageAssetAndKeyBase()
    val storedBytes = storedAsset.renderFromMaster.asByteArray(ImageAsset.Png)
    storedAsset.save()

    val restoredAsset = ImageAsset(keyBase, assetName, ImageAsset.Png, assetServices)
    val restoredBytes = restoredAsset.renderFromMaster.asByteArray(ImageAsset.Png)

    restoredBytes.toSeq should be (storedBytes.toSeq)
  }

  it should "throw an IllegalStateException when trying to source from a master blob that doesn't exist" in new EgraphsTestApplication {
    val keyBase = RandomStringUtils.random(10)
    val restoredAsset = ImageAsset(keyBase, assetName, ImageAsset.Png, assetServices) // important that it doesn't have master data parameter defined
    evaluating { restoredAsset.renderFromMaster } should produce [IllegalStateException]
  }

  it should "correctly store and fetch permutations as {name}/{width}x{height}" in new EgraphsTestApplication {
    val asset = newImageAsset()

    val resized = asset.resized(100, 100)
    val resizedBytes = resized.renderFromMaster.asByteArray(ImageAsset.Png)

    resized.fetchImage should be (None)

    resized.save()
    
    resized.fetchImage.get.asByteArray(ImageAsset.Png).toSeq should be (resizedBytes.toSeq)
  }

  it should "return the correct url" in new EgraphsTestApplication {
    val asset = newImageAsset()
    asset.save(Private)
    asset.url should include (asset.key)
  }

  it should "only provide a urlOption if the data are available" in new EgraphsTestApplication {
    val asset = newImageAsset()

    asset.urlOption should be (None)
    asset.save().urlOption should be (Some(asset.url))
  }

  def newImageAssetAndKeyBase(bytes: => Array[Byte] = imageBytes): (ImageAsset, String) = {
    val keyBase = RandomStringUtils.random(10)
    (ImageAsset(bytes, keyBase, assetName, ImageAsset.Png, assetServices), keyBase)
  }
  
  def newImageAsset(bytes: => Array[Byte] = imageBytes): ImageAsset = {
    val (imageAsset, _) = newImageAssetAndKeyBase(bytes)
    imageAsset
  }
}
