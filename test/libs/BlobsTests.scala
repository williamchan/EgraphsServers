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
  "Blobs" should "put and get data" in {
    val blob = "I herp then I derp".getBytes
    Blobs.put("myKey", blob)

    val restored = Blobs.get("myKey")

    restored.get.toSeq should be (blob.toSeq)
  }

  it should "not find data that don't exist" in {
    Blobs.get("herp") should be (None)
  }

  it should "have the most recent version of the blob" in {
    val key = "myKey"

    Blobs.put(key, "herp".getBytes)
    Blobs.put(key, "derp".getBytes)

    Blobs.get(key).get.toSeq should be ("derp".getBytes.toSeq)
  }

  it should "delete properly" in {
    val key = "myKey"

    Blobs.put(key, "herp".getBytes)
    Blobs.delete(key)

    Blobs.get(key) should be (None)
  }
}