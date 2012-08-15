package services.blobs

import play.Play.configuration
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}

class S3BlobVendorTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {
  val bucket = "egraphs-unit-test"

  "S3BlobVendor" should "not return bad URLs" in {
    withTestBucket { blobVendor =>
      blobVendor
    }
  }

  def withTestBucket[A](operation: S3BlobVendor => A): A = {
    val blobVendor = new S3BlobVendor(configuration.getProperty("s3.id"), configuration.getProperty("s3.secret"))

    val blobStore = blobVendor.context.getBlobStore

    if (!blobStore.containerExists(bucket)) {
      blobStore.createContainerInLocation(null, bucket)
    }

    blobStore.clearContainer(bucket)

    operation(blobVendor)
  }
}
