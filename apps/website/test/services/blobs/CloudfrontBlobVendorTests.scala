package services.blobs

import utils.{EgraphsUnitTest, ClearsCacheBefore}
import play.api.Play._
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CloudfrontBlobVendorTests extends EgraphsUnitTest with ClearsCacheBefore {
  private def cloudfrontDomain = "egraphs-test.edge.egraphs.com"

  "cloudfrontVendor" should "not return bad Urls" in {
    val key ="derp"
    val namespace ="herp"

    val blobVendor = mock[BlobVendor]
    blobVendor.urlOption(namespace, key) returns Some("https://localhost:9000/blob/files/derp")
    
    val cloudfrontVendor = new CloudfrontBlobVendor(cloudfrontDomain, blobVendor)

    cloudfrontVendor.urlOption(namespace, key) should be (Some("https://egraphs-test.edge.egraphs.com/blob/files/derp"))
  }
}
