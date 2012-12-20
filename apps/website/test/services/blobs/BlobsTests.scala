package services.blobs

import utils.{TestHelpers, ClearsCacheBefore, EgraphsUnitTest, DBTransactionPerTest}
import services.AppConfig
import services.Time
import Time.IntsToSeconds._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTimeConstants

@RunWith(classOf[JUnitRunner])
class BlobsTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with DBTransactionPerTest
{
  import Blobs.Conversions._

  def blobs = AppConfig.instance[Blobs]
  def newBlobKey = RandomStringUtils.randomAlphabetic(30)

  "Blobs" should "put and get data" in new EgraphsTestApplication {
    val blobKey = newBlobKey
    blobs.put(blobKey, "I herp then I derp")

    blobs.get(blobKey).get.asString should be ("I herp then I derp")
  }

  it should "not find data that don't exist" in new EgraphsTestApplication {
    blobs.get(newBlobKey) should be (None)
  }

  it should "have the most recent version of the blob" in new EgraphsTestApplication {
    val key = newBlobKey

    blobs.put(key, "herp")
    blobs.put(key, "derp")

    blobs.get(key).get.asString should be ("derp")
  }

  it should "delete properly" in new EgraphsTestApplication {
    val key = newBlobKey

    blobs.put(key, "herp")
    blobs.delete(key)

    blobs.get(key) should be (None)
  }

  "getStaticResourceUrl" should "return short-term signed URL to static S3 resource" in new EgraphsTestApplication {
    val expirationSeconds = 5 minutes
    val blobKey = newBlobKey
    val signedUrl = blobs.getStaticResourceUrl(key = blobKey, expirationSeconds = 5 minutes)
    val expires = (System.currentTimeMillis() / DateTimeConstants.MILLIS_PER_SECOND) + expirationSeconds

    val (url, queryParams) = TestHelpers.splitUrl(signedUrl)
    url should be("https://egraphs-static-resources.s3.amazonaws.com/" + blobKey)
    queryParams(0)._1 should be("AWSAccessKeyId")
    queryParams(0)._2 should be("AKIAJ33ZTKZIPYXRC66A")
    queryParams(1)._1 should be("Expires")
    queryParams(1)._2.toLong should be(expires plusOrMinus 5)
    queryParams(2)._1 should be("Signature")
    queryParams(2)._2.length should be > (0)
  }

  "RichBlob" should "convert properly" in new EgraphsTestApplication {
    val data = Array('h', 'e', 'r', 'p').map(theChar => theChar.toByte)

    val blobKey = newBlobKey
    blobs.put(blobKey, data)

    val restored = blobs.get(blobKey).get

    // String
    blobs.get(blobKey).get.asString should be ("herp")

    // Int stream
    blobs.get(blobKey).get.asIntStream.toSeq should be (data.toSeq)

    // Input stream
    val arr = new Array[Byte](4)
    blobs.get(blobKey).get.asInputStream.read(arr)
    arr.toSeq should be (data.toSeq)
  }
}