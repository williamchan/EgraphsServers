package services.blobs

import com.google.inject.{Inject, Provider}
import models.BlobKeyStore
import services.http.PlayConfig
import java.util.Properties
import services.cache.CacheFactory

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider @Inject() (
  cacheFactory: CacheFactory,
  blobKeyStore: BlobKeyStore,
  @PlayConfig playConfig: Properties
) extends Provider[BlobVendor]
{
  private val blobstoreType = playConfig.getProperty(Blobs.blobstoreConfigKey)

  def get() = {
    blobstoreType match {
      case "s3" =>
        new CacheIndexedBlobVendor(cacheFactory, blobKeyStore, S3BlobVendor)

      case "filesystem" =>
        new CacheIndexedBlobVendor(cacheFactory, blobKeyStore, FileSystemBlobVendor)

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }
}





