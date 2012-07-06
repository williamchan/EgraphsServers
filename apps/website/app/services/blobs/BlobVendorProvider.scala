package services.blobs

import com.google.inject.Provider
import services.AppConfig
import models.BlobKeyStore

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider(blobstoreType: String) extends Provider[BlobVendor] {
  // TODO: Don't use AppConfig like this. Instead take it as a dependency.
  private val blobKeyStore = AppConfig.instance[BlobKeyStore]

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





