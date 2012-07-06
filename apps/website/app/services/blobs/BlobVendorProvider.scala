package services.blobs

import com.google.inject.Provider

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */
private[blobs] class BlobVendorProvider(blobstoreType: String) extends Provider[BlobVendor] {
  def get() = {
    blobstoreType match {
      case "s3" =>
        DBIndexedS3BlobVendor

      case "filesystem" =>
        DBIndexedFileSystemBlobVendor

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }
}





