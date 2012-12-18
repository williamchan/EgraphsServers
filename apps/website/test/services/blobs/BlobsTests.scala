package services.blobs

import utils.{TestHelpers, ClearsCacheBefore, EgraphsUnitTest, DBTransactionPerTest}
import services.AppConfig
import services.Time
import Time.IntsToSeconds._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BlobsTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with DBTransactionPerTest
{
  import Blobs.Conversions._

  def blobs = AppConfig.instance[Blobs]

  "Blobs" should "put and get data" in new EgraphsTestApplication {
    blobs.put("myKey", "I herp then I derp")

    blobs.get("myKey").get.asString should be ("I herp then I derp")
  }

  it should "not find data that don't exist" in new EgraphsTestApplication {
    blobs.get("herp") should be (None)
  }

  it should "have the most recent version of the blob" in new EgraphsTestApplication {
    val key = "myKey"

    blobs.put(key, "herp")
    blobs.put(key, "derp")

    blobs.get("myKey").get.asString should be ("derp")
  }

  it should "delete properly" in new EgraphsTestApplication {
    val key = "myKey"

    blobs.put(key, "herp")
    blobs.delete(key)

    blobs.get("myKey") should be (None)
  }

  "getStaticResourceUrl" should "return short-term signed URL to static S3 resource" in new EgraphsTestApplication {
    val expirationSeconds = 5 minutes
    val signedUrl = blobs.getStaticResourceUrl(key = "derp", expirationSeconds = 5 minutes)
    val expires = System.currentTimeMillis() / 1000 + expirationSeconds

    val urlAndQueryParams = TestHelpers.splitUrl(signedUrl)
    urlAndQueryParams._1 should be("https://egraphs-static-resources.s3.amazonaws.com/derp")
    val queryParams = urlAndQueryParams._2
    queryParams(0)._1 should be("AWSAccessKeyId")
    queryParams(0)._2 should be("AKIAJ33ZTKZIPYXRC66A")
    queryParams(1)._1 should be("Expires")
    queryParams(1)._2.toLong should be(expires plusOrMinus 5)
    queryParams(2)._1 should be("Signature")
    queryParams(2)._2.length should be > (0)
  }

  "RichBlob" should "convert properly" in new EgraphsTestApplication {
    val data = Array('h', 'e', 'r', 'p').map(theChar => theChar.toByte)

    blobs.put("herp", data)

    val restored = blobs.get("herp").get

    // String
    blobs.get("herp").get.asString should be ("herp")

    // Int stream
    blobs.get("herp").get.asIntStream.toSeq should be (data.toSeq)

    // Input stream
    val arr = new Array[Byte](4)
    blobs.get("herp").get.asInputStream.read(arr)
    arr.toSeq should be (data.toSeq)
  }
}