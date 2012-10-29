package services.blobs

import com.google.inject.{Inject, Provider}
import models.BlobKeyStore
import services._
import services.cache.CacheFactory
import services.inject.InjectionProvider
import services.config.ConfigFileProxy

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider @Inject() (
  blobKeyStore: BlobKeyStore,
  cacheFactory: CacheFactory,
  config: ConfigFileProxy,
  val s3: S3BlobVendor,
  consumerApp: ConsumerApplication
) extends InjectionProvider[BlobVendor]
{
  private val blobstoreType = config.blobstoreVendor
  private val maybeCdnDomain = config.cdnContentUrl

  def get() = {
    blobstoreType match {
      case "s3" =>
        maybeCdnDomain match {
          case Some(cdnDomain) => decorateCDN(decorateCache(s3), cdnDomain)
          case _      => decorateCache(s3)
        }

      case "filesystem" =>
        decorateCache(new FileSystemBlobVendor(consumerApp))

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }
  
  //
  // Private members
  //
  private def decorateCache(baseBlobVendor: BlobVendor): BlobVendor = {
    new CacheIndexedBlobVendor(cacheFactory, baseBlobVendor)
  }

  private def decorateCDN(baseBlobVendor: BlobVendor, cdnDomain: String): BlobVendor = {
    new CloudfrontBlobVendor(cdnDomain, baseBlobVendor)
  }
}
