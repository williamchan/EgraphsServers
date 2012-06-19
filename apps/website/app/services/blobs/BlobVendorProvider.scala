package services.blobs

import play.Play.configuration
import com.google.inject.Provider

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider(blobstoreType: String) extends Provider[BlobVendor] {
  def get() = {
    blobstoreType match {
      case "s3" =>
        S3BlobVendor

      case "filesystem" =>
        FileSystemBlobVendor

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }
}





