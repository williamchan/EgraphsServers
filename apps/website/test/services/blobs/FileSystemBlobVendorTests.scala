package services.blobs

import utils.EgraphsUnitTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.AppConfig

@RunWith(classOf[JUnitRunner])
class FileSystemBlobVendorTests extends EgraphsUnitTest {
  import Blobs.Conversions._

  private val underTest = AppConfig.instance[FileSystemBlobVendor]

  val blobStore = underTest.context.getBlobStore

  "FileSystemBlobVendor" should "store and retrieve a blob" in {
    val blobBytes = "Herp".getBytes()

    underTest.put("test", "herp", blobBytes, AccessPolicy.Public)

    val retrieved = blobStore.getBlob("test", "herp")
    retrieved.asByteArray should be (blobBytes)

    blobStore.clearContainer("test")
  }

  it should "not retrieve blobs that don't exist" in {
    blobStore.getBlob("test", "derp") should be (null)
  }

  it should "not have a url for blobs that don't exist" in {
    underTest.urlOption("test", "derp") should be (None)
  }

  it should "have a url for blobs that exist" in {
    underTest.put("test", "herp", "derpderp".getBytes, AccessPolicy.Public)

    underTest.urlOption("test", "herp") should not be (None)

    blobStore.clearContainer("test")

  }
}

trait BlobVendorTests { this: EgraphsUnitTest =>

  def testVendor: BlobVendor

}