package services.blobs

import utils.EgraphsUnitTest
import play.Play._
import play.Play.configuration

class S3BlobVendorTests extends EgraphsUnitTest {
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

    try {
      operation(blobVendor)
    }
    finally {
      blobStore.deleteContainer(bucket)
    }
  }
}
