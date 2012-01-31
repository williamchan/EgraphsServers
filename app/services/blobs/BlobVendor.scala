package services.blobs

import java.util.Properties
import org.jclouds.filesystem.reference.FilesystemConstants
import play.Play.configuration
import org.jclouds.blobstore.{BlobStoreContextFactory, BlobStoreContext, BlobStore}
import org.jclouds.blobstore.domain.Blob
import org.jclouds.io.Payload
import org.jclouds.aws.s3.AWSS3Client
import org.jclouds.s3.domain.CannedAccessPolicy
import java.io._
import play.mvc.Http.Request
import com.google.inject.Inject

/**
 * Interface for different BlobStore implementations.
 */
private[blobs] trait BlobVendor {
  /** Instantiates a new configured BlobStoreContext */
  def context: BlobStoreContext

  /** Returns the base URL for accessing [[libs.AccessPolicy.Public]] resources */
  def urlBase: String

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
}
