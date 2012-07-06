package services.blobs

import com.google.inject.{Inject, Provider}
import services.AppConfig
import models.BlobKeyStore
import services.http.PlayConfig
import java.util.Properties

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider @Inject() (
  blobKeyStore: BlobKeyStore,
  @PlayConfig playConfig: Properties
) extends Provider[BlobVendor]
{
  private val blobstoreType = playConfig.getProperty(Blobs.blobstoreConfigKey)

  def get() = {
    blobstoreType match {
      case "s3" =>
        new DBIndexedBlobVendor(blobKeyStore, S3BlobVendor)

      case "filesystem" =>
        new DBIndexedBlobVendor(blobKeyStore, FileSystemBlobVendor)

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }
}





