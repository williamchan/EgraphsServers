package models

import services.AppConfig
import utils._
import org.apache.commons.lang.RandomStringUtils

class BlobKeyTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[BlobKey]
  with CreatedUpdatedEntityTests[Long, BlobKey]
  with DateShouldMatchers
  with DBTransactionPerTest
{

  def randomKey = RandomStringUtils.randomAlphanumeric(50)

  def store = AppConfig.instance[BlobKeyStore]

  //
  // SavingEntityTests[BlobKey] methods
  //
  override def newEntity = {
    BlobKey( key = randomKey )
  }

  override def saveEntity(toSave: BlobKey) = {
    store.save(toSave)
  }

  override def restoreEntity(blobKeyid: Long) = {
    store.findById(blobKeyid)
  }

  override def transformEntity(toTransform: BlobKey) = {
    toTransform.copy(
      key = RandomStringUtils.randomAlphanumeric(50),
      url = "www.google.com"
    )
  }

  "key" should "be unique" in new EgraphsTestApplication {
    val saved = BlobKey(key = randomKey, url = "www.egraphs.com").save()
    // TODO - refactor this and other tests with this pattern for checking PSQLException
    val exception = intercept[RuntimeException] {
      BlobKey(key = saved.key, url = saved.url).save()
    }
    val psqlException = exception.getCause
    psqlException.getClass.getCanonicalName should be("org.postgresql.util.PSQLException")
    psqlException.getLocalizedMessage should startWith("ERROR: duplicate key value violates unique constraint \"idx1810041f\"")
  }

  "findByKey" should "find by Key!" in new EgraphsTestApplication {
    val testKey = randomKey
    store.findByKey(testKey) should be(None)
    val blobKey = BlobKey(key = testKey, url = "www.egraphs.com").save()
    store.findByKey(testKey).get should be(blobKey)
  }
}
