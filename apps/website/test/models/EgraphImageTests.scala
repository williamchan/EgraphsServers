package models

import services.AppConfig
import services.blobs.{Blobs, AccessPolicy}
import Blobs.Conversions._
import utils._

class EgraphImageTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
{

  private val blobs = AppConfig.instance[Blobs]

  "saveAndGetUrl" should "save Egraph image at url with handwriting rendered on product photo" in {
    val product = TestData.newSavedProduct()
    val order = TestData.newSavedOrder(Some(product))
    val egraph = order.newEgraph.withAssets(TestConstants.signatureStr, Some(TestConstants.messageStr), TestConstants.fakeAudio).save()

    val rawSignedImage = egraph.image(product.photoImage)
    val frameFittedImage = rawSignedImage.scaledToWidth(LandscapeEgraphFrame.imageWidthPixels)
    val frameFittedImageUrl = frameFittedImage.saveAndGetUrl(AccessPolicy.Public)
    val blobKey = TestHelpers.getBlobKeyFromTestBlobUrl(frameFittedImageUrl)

    blobs.get(blobKey).get.asByteArray.length should be(59933) // If this breaks, check the rendered image
  }

  "saveAndGetUrl" should "use signing origin offsets" in {
    val product = TestData.newSavedProduct()
    val order = TestData.newSavedOrder(Some(product))
    val egraph = order.newEgraph.withAssets(TestConstants.signatureStr, Some(TestConstants.messageStr), TestConstants.fakeAudio).save()

    val rawSignedImage = egraph.image(product.photoImage)
    val frameFittedImage = rawSignedImage
      .withSigningOriginOffset(100.0, 100.0)
      .scaledToWidth(LandscapeEgraphFrame.imageWidthPixels)
    val frameFittedImageUrl = frameFittedImage.saveAndGetUrl(AccessPolicy.Public)
    val blobKey = TestHelpers.getBlobKeyFromTestBlobUrl(frameFittedImageUrl)

    blobs.get(blobKey).get.asByteArray.length should be(59975) // If this breaks, check the rendered image
  }

//  TODO: single-point strokes are not being rendered, see issue #120
//  "saveAndGetUrl" should "render single-point stroke" in {
//    val product = TestData.newSavedProduct()
//    val order = TestData.newSavedOrder(Some(product))
//    val stroke = "{\"x\":[[67]],\"y\":[[198]],\"t\":[[324217524]]}"
//    val egraph = order.newEgraph.withAssets(stroke, None, TestConstants.fakeAudio).save()
//
//    val rawSignedImage = egraph.image(product.photoImage)
//    val frameFittedImage = rawSignedImage.scaledToWidth(LandscapeEgraphFrame.imageWidthPixels)
//    val frameFittedImageUrl = frameFittedImage.saveAndGetUrl(AccessPolicy.Public)
//    val blobKey = TestHelpers.getBlobKeyFromTestBlobUrl(frameFittedImageUrl)
//
//    blobs.get(blobKey).get.asByteArray.length should be(49252) // If this breaks, check the rendered image
//  }

  "saveAndGetUrl" should "render two-point stroke" in {
    val product = TestData.newSavedProduct()
    val order = TestData.newSavedOrder(Some(product))
    val stroke = "{\"x\":[[67,80]],\"y\":[[198,220]],\"t\":[[324217524,341077816]]}"
    val egraph = order.newEgraph.withAssets(stroke, None, TestConstants.fakeAudio).save()

    val rawSignedImage = egraph.image(product.photoImage)
    val frameFittedImage = rawSignedImage.scaledToWidth(LandscapeEgraphFrame.imageWidthPixels)
    val frameFittedImageUrl = frameFittedImage.saveAndGetUrl(AccessPolicy.Public)
    val blobKey = TestHelpers.getBlobKeyFromTestBlobUrl(frameFittedImageUrl)

    blobs.get(blobKey).get.asByteArray.length should be(49270) // If this breaks, check the rendered image
  }

  "saveAndGetUrl" should "render three-point stroke" in {
    val product = TestData.newSavedProduct()
    val order = TestData.newSavedOrder(Some(product))
    val stroke = "{\"x\":[[67,80,95]],\"y\":[[198,220,250]],\"t\":[[324217524,341077816,356683482]]}"
    val egraph = order.newEgraph.withAssets(stroke, None, TestConstants.fakeAudio).save()

    val rawSignedImage = egraph.image(product.photoImage)
    val frameFittedImage = rawSignedImage.scaledToWidth(LandscapeEgraphFrame.imageWidthPixels)
    val frameFittedImageUrl = frameFittedImage.saveAndGetUrl(AccessPolicy.Public)
    val blobKey = TestHelpers.getBlobKeyFromTestBlobUrl(frameFittedImageUrl)

    blobs.get(blobKey).get.asByteArray.length should be(49292) // If this breaks, check the rendered image
  }

}
