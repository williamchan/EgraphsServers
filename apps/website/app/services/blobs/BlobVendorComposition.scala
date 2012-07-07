package services.blobs

import org.jclouds.blobstore.domain.Blob

/**
 * Class that enables easy composition of BlobVendors.
 *
 * Usage:
 * {{{
 *   class MyNewBlobVendor(protected val blobVendorDelegate: BlobVendor) extends BlobVendor with BlobVendorComposition {
 *     // Only override the members of BlobVendor that you don't want to delegate
 *     // to blobVendorDelegate within this class. By default it delegates the entire interface.
 *   }
 * }}}
 */
private[blobs] trait BlobVendorComposition extends BlobVendor {

  /**
   * Object to which to delegate the BlobVendor interface
   */
  protected def blobVendorDelegate: BlobVendor

  override def get(namespace: String, key: String): Option[Blob] = {
    blobVendorDelegate.get(namespace, key)
  }

  def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    blobVendorDelegate.put(namespace, key, data, access)
  }

  override def exists(namespace: String, key: String) = {
    blobVendorDelegate.exists(namespace, key)
  }

  def urlOption(namespace: String, key: String) = {
    blobVendorDelegate.urlOption(namespace, key)
  }
}
