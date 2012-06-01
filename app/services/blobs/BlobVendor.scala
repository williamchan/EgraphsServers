package services.blobs

import org.jclouds.blobstore.BlobStoreContext

/**
 * Interface for different BlobStore implementations.
 */
private[blobs] trait BlobVendor {
  /** Instantiates a new configured BlobStoreContext */
  def context: BlobStoreContext

  /**
   * Checks that the blobstore implementation is correctly configured, and throws runtime
   * exceptions if not.
   */
  def checkConfiguration()

  /**
   * Puts an object into a key with a particular namespace.
   *
   * @param namespace the key namespace -- equivalent to an Amazon S3 bucket or a file-system folder.
   *
   * @param key the unique key name against which to store the object.
   *
   * @param data the object data to store.
   */
  def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy)

  /**
   * Gets the URL for the resource if the resource exists.
   *
   * @param namespace the key namespace -- equivalent to an Amazon S3 bucket or a file-system folder
   * @param key the unique key name against which to store the object
   * @return Some(URL) if the resource was found, otherwise None.
   */
  def urlOption(namespace: String, key: String): Option[String]
}
