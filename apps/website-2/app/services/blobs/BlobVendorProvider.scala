package services.blobs

import com.google.inject.{Inject, Provider}
import models.BlobKeyStore
import play.api.Configuration
import services.cache.CacheFactory
import services.inject.InjectionProvider

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider @Inject() (
  blobKeyStore: BlobKeyStore,
  cacheFactory: CacheFactory,
  playConfig: Configuration
) extends InjectionProvider[BlobVendor]
{
  private val blobstoreType = playConfig.getString(Blobs.blobstoreConfigKey).get
  private val cloudfrontDomain = playConfig.getString("cloudfront.domain").get
  private val cdnEnabled = playConfig.getString("cdn.enabled").get

  def get() = {
    blobstoreType match {
      case "s3" =>
        cdnEnabled match {
          case "true" => decorateCDN(decorateCache(s3))
          case _      => decorateCache(s3)
        }

      case "filesystem" =>
        decorateCache(FileSystemBlobVendor)

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }
  
  def s3: S3BlobVendor = {
    S3BlobVendor(playConfig)
  }

  //
  // Private members
  //
  private def decorateCache(baseBlobVendor: BlobVendor): BlobVendor = {
    new CacheIndexedBlobVendor(cacheFactory, baseBlobVendor)
  }

  private def decorateCDN(baseBlobVendor: BlobVendor): BlobVendor = {
    new CloudfrontBlobVendor(cloudfrontDomain, baseBlobVendor)
  }
}
