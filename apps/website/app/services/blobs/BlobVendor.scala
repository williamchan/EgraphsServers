package services.blobs

import org.jclouds.blobstore.{BlobStore, BlobStoreContext}
import org.jclouds.blobstore.domain.Blob
import services.Time.IntsToSeconds._

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

  def get(namespace: String, key: String): Option[Blob] = {
    Option(blobStore.getBlob(namespace, key))
  }

  /**
   * Deletes an object with the particular namespace and key.
   * 
   * @param namespace the key namespace -- equivalent to an Amazon S3 bucket or a file-system folder.
   *
   * @param key the unique key name against which the object is stored.
   */
  def delete(namespace: String, key: String)

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
  
  /**
   * Gets the secure URL (with access key and expiration time, where appropriate) for the resource if the resource exists.
   * 
   * @param namespace the key namespace -- equivalent to an Amazon S3 bucket or a file-system folder
   * @param key the unique key name against which to store the object
   * @param expirationSeconds the number of seconds that this secure URL will be accessible
   * @return Some(URL) if the resource was found, otherwise None.
   */
  def secureUrlOption(namespace: String, key: String, expirationSeconds: Int = 5 minutes): Option[String] = {
    urlOption(namespace, key)
  }

  /**
   * Tests that there exists a value for the key in the specified namespace.
   *
   * @param namespace the key namespace -- equivalent to an Amazon S3 bucket or a file-system folder
   * @param key the unique key name against which an object may be stored
   * @return true that a value exists for the namespace and key
   */
  def exists(namespace: String, key: String): Boolean = {
    blobStore.blobExists(namespace, key)
  }

  /**
   * Returns the underlying jcloud BlobStore implementation for this BlobVendor.
   */
  private def blobStore: BlobStore = {
    context.getBlobStore
  }
}
