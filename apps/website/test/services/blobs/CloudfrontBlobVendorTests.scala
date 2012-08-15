package services.blobs

import utils.{EgraphsUnitTest, ClearsCacheAndBlobsAndValidationBefore}
import play.Play._

class CloudfrontBlobVendorTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {
  private val cloudfrontDomain = configuration.getProperty("cloudfront.domain")

  "cloudfrontVendor" should "not return bad Urls" in {
    val blobVendor = new FileSystemBlobVendor()
    val cloudfrontVendor = new CloudfrontBlobVendor(cloudfrontDomain, blobVendor)

    val key ="derp"
    val namespace ="herp"
    val bytes = Array(0xff, 0x22, 0xcd).map(_.toByte)

    cloudfrontVendor.put(namespace, key, bytes , AccessPolicy.Public)

    val urlOption = cloudfrontVendor.urlOption(namespace, key)
    urlOption.get should be("https://egraphs-test.edge.egraphs.com/blob/files/derp")
  }
}
