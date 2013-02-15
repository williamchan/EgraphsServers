package services.blobs

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.EgraphsUnitTest
import services.AppConfig

@RunWith(classOf[JUnitRunner])
class FileSystemBlobVendorTests extends EgraphsUnitTest {
  import Blobs.Conversions._

  private def underTest = AppConfig.instance[FileSystemBlobVendor]

  private def blobStore = underTest.context.getBlobStore

  "FileSystemBlobVendor" should "store and retrieve a blob" in new EgraphsTestApplication {
    val blobBytes = "Herp".getBytes()

    underTest.put("test", "herp", blobBytes, AccessPolicy.Public)

    val retrieved = blobStore.getBlob("test", "herp")
    retrieved.asByteArray should be (blobBytes)

    blobStore.clearContainer("test")
  }

  it should "not retrieve blobs that don't exist" in new EgraphsTestApplication {
    blobStore.getBlob("test", "derp") should be (null)
  }

  it should "not have a url for blobs that don't exist" in new EgraphsTestApplication {
    underTest.urlOption("test", "derp") should be (None)
  }

  it should "have a url for blobs that exist" in new EgraphsTestApplication {
    underTest.put("test", "herp", "derpderp".getBytes, AccessPolicy.Public)

    underTest.urlOption("test", "herp") should not be (None)

    blobStore.clearContainer("test")

  }
}

trait BlobVendorTests { this: EgraphsUnitTest =>

  def testVendor: BlobVendor

}