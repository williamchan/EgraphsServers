package services.blobs

import com.google.inject.Inject
import models.BlobKeyStore
import services.cache.CacheFactory
import services.Time

class CacheIndexedBlobVendor @Inject()(cacheFactory: CacheFactory, blobKeyStore: BlobKeyStore, override protected val blobVendorDelegate: BlobVendor)
  extends DBIndexedBlobVendor(blobKeyStore, blobVendorDelegate)
{
  private val cache = cacheFactory.applicationCache

  private def cacheKey(namespace: String, key: String) = namespace+"/"+key

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    super.put(namespace, key, data, access)
    import Time.IntsToSeconds._
    blobVendorDelegate.urlOption(namespace, key).map { url =>
      cache.set(cacheKey(namespace, key), url, 3.days)
    }
  }

  override def exists(namespace: String, key: String) = {
    cache.get[String](cacheKey(namespace, key)) match {
      case Some(x) => true
      case _ => super.exists(namespace, key)
    }
  }

  override def urlOption(namespace: String, key: String) = {
    cache.get[String](cacheKey(namespace, key)) match {
      case Some(url) if Option(url).isDefined  => Some(url)
      case _ => super.urlOption(namespace, key)
    }
  }
}
