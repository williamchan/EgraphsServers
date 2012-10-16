package services.blobs

import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import services.AppConfig
import services.config.ConfigFileProxy
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class S3BlobVendorTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {
  "S3BlobVendor" should "not return bad URLs" in {
    withTestBucket { blobVendor =>
      blobVendor
    }
  }

  // Tests Query String Authentication Example from http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html
  "sign" should "return signature" in {
    val exampleSecretKey = "OtxrzxIsfpFjA7SwPzILwy8Bw21TLhquhboDYROV"
    val expectedSignature	= "vjbyPxybdZaNmGa%2ByT272YEAiv4%3D"

    this.blobVendor.sign(
      namespace = "quotes", 
      key = "nelson", 
      expires = 1141889120, 
      awsSecretKey = exampleSecretKey
    ) should be(expectedSignature)
  }

  //
  // Private members
  //
  def withTestBucket[A](operation: S3BlobVendor => A): A = {    
    val blobVendor = new S3BlobVendor(config.s3Id, config.s3Secret)

    val blobStore = blobVendor.context.getBlobStore

    if (!blobStore.containerExists(bucket)) {
      blobStore.createContainerInLocation(null, bucket)
    }

    blobStore.clearContainer(bucket)

    operation(blobVendor)
  }
  
  //
  // Private members
  //
  val bucket = "egraphs-unit-test"
  def blobVendor = new S3BlobVendor(config.s3Id, config.s3Secret)
  def config = AppConfig.instance[ConfigFileProxy]
}
