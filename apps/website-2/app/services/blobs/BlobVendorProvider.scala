package services.blobs

import com.google.inject.{Inject, Provider}
import models.BlobKeyStore
import services.http.PlayConfig
import java.util.Properties
import services.cache.CacheFactory
import services.inject.InjectionProvider

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider @Inject() (
  blobKeyStore: BlobKeyStore,
  cacheFactory: CacheFactory,
  @PlayConfig playConfig: Properties
) extends InjectionProvider[BlobVendor]
{
  private val blobstoreType = playConfig.getProperty(Blobs.blobstoreConfigKey)
  private val cloudfrontDomain = playConfig.getProperty("cloudfront.domain")
  private val cdnEnabled = playConfig.getProperty("cdn.enabled")

  def get() = {
    blobstoreType match {
      case "s3" =>
        cdnEnabled match {
          case "true" => decorateCDN(decorateCache(S3BlobVendor))
          case _      => decorateCache(S3BlobVendor)
        }

      case "filesystem" =>
        decorateCache(FileSystemBlobVendor)

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }

  private def decorateCache(baseBlobVendor: BlobVendor): BlobVendor = {
    new CacheIndexedBlobVendor(cacheFactory, baseBlobVendor)
  }

  private def decorateCDN(baseBlobVendor: BlobVendor): BlobVendor = {
    new CloudfrontBlobVendor(cloudfrontDomain, baseBlobVendor)
  }
}
