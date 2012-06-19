package services.blobs

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import org.scalatest.BeforeAndAfterEach
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationBefore}
import services.AppConfig

class BlobsTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
{
  import Blobs.Conversions._
  val blobs = AppConfig.instance[Blobs]

  "Blobs" should "put and get data" in {
    blobs.put("myKey", "I herp then I derp")

    blobs.get("myKey").get.asString should be ("I herp then I derp")
  }

  it should "not find data that don't exist" in {
    blobs.get("herp") should be (None)
  }

  it should "have the most recent version of the blob" in {
    val key = "myKey"

    blobs.put(key, "herp")
    blobs.put(key, "derp")

    blobs.get("myKey").get.asString should be ("derp")
  }

  it should "delete properly" in {
    val key = "myKey"

    blobs.put(key, "herp")
    blobs.delete(key)

    blobs.get("myKey") should be (None)
  }

  "RichBlob" should "convert properly" in {
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