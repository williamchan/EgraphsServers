package services.blobs

import utils.{ClearsDatabaseAndValidationBefore, EgraphsUnitTest}
import services.cache.CacheFactory

class CacheIndexedBlobVendorTests extends EgraphsUnitTest with ClearsDatabaseAndValidationBefore {

  "put" should "not cache if url is not available" in {
    val namespace = "testnamespace"
    val key = "herp"
    val blob = "blob"

    val blobVendorWithoutBlobs = mock[BlobVendor]
    blobVendorWithoutBlobs.urlOption(namespace, key) returns None
    val cacheFactory = mock[CacheFactory]

    val cacheIndexedBlobVendor = new CacheIndexedBlobVendor(cacheFactory, blobVendorWithoutBlobs)
    cacheIndexedBlobVendor.put(namespace, key, blob.getBytes, AccessPolicy.Public)
    there was no(cacheFactory).applicationCache // if applicationCache was never called, then nothing was ever cached
  }

}