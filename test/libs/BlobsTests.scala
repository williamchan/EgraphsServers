package libs

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import org.scalatest.BeforeAndAfterEach
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter}

class BlobsTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  import Blobs.Conversions._

  "Blobs" should "put and get data" in {
    Blobs.put("myKey", "I herp then I derp")

    Blobs.get("myKey").get.asString should be ("I herp then I derp")
  }

  it should "not find data that don't exist" in {
    Blobs.get("herp") should be (None)
  }

  it should "have the most recent version of the blob" in {
    val key = "myKey"

    Blobs.put(key, "herp")
    Blobs.put(key, "derp")

    Blobs.get("myKey").get.asString should be ("derp")
  }

  it should "delete properly" in {
    val key = "myKey"

    Blobs.put(key, "herp")
    Blobs.delete(key)

    Blobs.get("myKey") should be (None)
  }

  "RichBlob" should "convert properly" in {
    val data = Array('h', 'e', 'r', 'p').map(theChar => theChar.toByte)

    Blobs.put("herp", data)

    val restored = Blobs.get("herp").get

    // String
    Blobs.get("herp").get.asString should be ("herp")

    // Int stream
    Blobs.get("herp").get.asIntStream.toSeq should be (data.toSeq)

    // Input stream
    val arr = new Array[Byte](4)
    Blobs.get("herp").get.asInputStream.read(arr)
    arr.toSeq should be (data.toSeq)
  }
}