package services.blobs

import com.google.inject.Inject
import models.BlobKeyStore
import services.cache.CacheFactory
import services.Time

class CacheIndexedBlobVendor @Inject()(cacheFactory: CacheFactory, override protected val blobVendorDelegate: BlobVendor)
  extends BlobVendorComposition
{
  private val cache = cacheFactory.applicationCache

  private def makeCacheKey(namespace: String, key: String) = namespace+"/"+key

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    blobVendorDelegate.put(namespace, key, data, access)

    blobVendorDelegate.urlOption(namespace, key).map { url =>
      cacheKeyAndUrl(namespace, key, url)
    }
  }

  override def exists(namespace: String, key: String) = {
    cache.get[String](makeCacheKey(namespace, key)) match {
      case Some(x) =>
        true

      case None =>
        // Instead of directly calling delegate exists method, check the url and cache it
        // if it's there.
        val blobVendorUrlOption = blobVendorDelegate.urlOption(namespace, key)
        blobVendorUrlOption.map(url => cacheKeyAndUrl(namespace, key, url))

        // If there was a URL go ahead and return true, otherwise directly test
        // delegate's exists method
        blobVendorUrlOption.map(url => true).getOrElse {
          blobVendorDelegate.exists(namespace, key)
        }
    }
  }

  override def urlOption(namespace: String, key: String) = {
    // Try to get the URL from the cache
    cache.get[String](makeCacheKey(namespace, key)).orElse {

      // We didn't find it in the cache. Try to get it from the delegate
      val delegateResponse = blobVendorDelegate.urlOption(namespace, key)

      // If it WAS in the delegate, cache it before returning what the delegate
      // just handed to us.
      delegateResponse.map(url => cacheKeyAndUrl(namespace, key, url))

      // Return the delegate's reponse
      delegateResponse
    }
  }

  // Caches the blob URL that maps to a namespace/key pair
  private def cacheKeyAndUrl(namespace: String, key: String, url: String) {
    import Time.IntsToSeconds._
    cache.set(makeCacheKey(namespace, key), url, 3.days)
  }
}
