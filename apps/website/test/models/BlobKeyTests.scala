package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import services.AppConfig.instance
import utils._

class BlobKeyTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[BlobKey]
  with CreatedUpdatedEntityTests[BlobKey]
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
{

  def store = instance[BlobKeyStore]

  //
  // SavingEntityTests[BlobKey] methods
  //
  override def newEntity = {
    BlobKey()
  }

  override def saveEntity(toSave: BlobKey) = {
    store.save(toSave)
  }

  override def restoreEntity(blobKeyid: Long) = {
    store.findById(blobKeyid)
  }

  override def transformEntity(toTransform: BlobKey) = {
    toTransform.copy(
      key = "derp",
      url = "www.google.com"
    )
  }

  "key" should "be unique" in {
    BlobKey(key = "herp", url = "www.egraphs.com").save()
    // TODO - refactor this and other tests with this pattern for checking PSQLException
    val exception = intercept[RuntimeException] {
      BlobKey(key = "herp", url = "www.egraphs.com").save()
    }
    val psqlException = exception.getCause
    psqlException.getClass.getCanonicalName should be("org.postgresql.util.PSQLException")
    psqlException.getLocalizedMessage should startWith("ERROR: duplicate key value violates unique constraint \"idx1810041f\"")
  }

  "findByKey" should "find by Key!" in {
    store.findByKey("herp") should be(None)
    val blobKey = BlobKey(key = "herp", url = "www.egraphs.com").save()
    store.findByKey("herp").get should be(blobKey)
  }
}
