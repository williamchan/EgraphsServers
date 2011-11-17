package libs

import org.jclouds.blobstore.{BlobStoreContextFactory, BlobStoreContext, BlobStore}
import java.util.Properties
import org.jclouds.filesystem.reference.FilesystemConstants
import play.Play.configuration

/**
 * Convenience methods for storing and loading large binary data: images,
 * audio, signatures, video, etc.
 */
object Blobs {
  /** Retrieve the raw bytes of a Blob at a given key */
  def get(key: String): Option[Array[Byte]] = {
    blobStore.getBlob(blobstoreNamespace, key) match {
      case null =>
        None

      case blob =>
        val blobStream = blob.getPayload.getInput
        val blobIntIterator = Iterator.continually(blobStream.read).takeWhile(nextByte => nextByte != -1)

        Some(blobIntIterator.map(theInt => theInt.toByte).toArray)
    }
  }

  /**
   * Puts data into a Blob object.
   *
   * @param key the key by which to store the data
   * @param bytes the data to store
   */
  def put(key: String, bytes: Array[Byte]) {
    val blob = blobStore.blobBuilder(key).payload(bytes).build()
    blobStore.putBlob(blobstoreNamespace, blob)
  }

  /** Delete the data at a certain key. */
  def delete(key: String) {
    blobStore.removeBlob(blobstoreNamespace, key)
  }

  /**
   * Clears the entire Blobstore. Only runs if "blobstore.allowscrub" is
   * set to "yes" in application.conf.
   */
  def scrub() {
    configuration.get("blobstore.allowscrub") match {
      case "yes" =>
        blobStore.clearContainer(blobstoreNamespace)

      case _ =>
        throw new IllegalStateException(
          """I'm not going to scrub the blobstore unless "blobstore.allowscrub"
          is set to "yes" in application.conf"""
        )
    }
  }

  /**
   * Retrieve the actual Blobstore when more intricate use of the
   * jclouds api is needed.
   */
  def blobStore: BlobStore = {
    context.getBlobStore
  }

  /**
   * Verifies configuration information and bootstraps the egraphs container
   * in the blobstore.
   */
  def init() = {
    checkAvailable()

    if (!blobStore.containerExists(blobstoreNamespace)) {
      blobStore.createContainerInLocation(null, blobstoreNamespace)
    }
  }

  /**
   * Retrieve the jclouds BlobStore context when more intricate use of the
   * jclouds api is needed.
   */
  def context: BlobStoreContext = {

    blobstoreType match {
      case "s3" =>
        s3Context

      case "filesystem" =>
        fileSystemContext

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }

  /**
   * Verifies that the blobstore is correctly configured, as far as it can
   * without actually trying to open the store.
   */
  def checkAvailable() {
    require(
      blobstoreType != null,
      """
      application.conf: A "blobstore" configuration value of either "s3" or
      "filesystem" is required
      """
    )

    require(
      blobstoreNamespace != null,
      """
      application.conf: A "blobstore.namespace" value is required. On s3 this
      is the bucket name.
      """
    )

    if(blobstoreType == "s3") {
      require(
        s3id != null,
        """
        application.conf: An "s3.id" configuration must be provided.
        """
      )
      require(
        s3secret != null,
        """
        application.conf: An "s3.secret" configuration must be provided.
        """
      )
    }
  }

  /** Type of blobstore. See "blobstore" in application.conf */
  private val blobstoreType = configuration.getProperty("blobstore")

  /** Namespace of blobstore; equivalent to S3's bucket */
  private val blobstoreNamespace = configuration.getProperty("blobstore.namespace")

  /** Amazon S3 public id */
  private val s3id = configuration.getProperty("s3.id")

  /** Amazon S3 secret key */
  private val s3secret = configuration.getProperty("s3.secret")

  /** Renders a BlobStoreContext implemented against Amazon S3 */
  private def s3Context: BlobStoreContext = {
    new BlobStoreContextFactory().createContext("aws-s3", s3id, s3secret)
  }

  /** Renders a BlobStoreContext implemented against the local filesystem */
  private def fileSystemContext: BlobStoreContext = {
    val properties = new Properties

    properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, "./public/blobstore")

    // jclouds borks if we don't provde any jclouds.credential string.
    properties.setProperty("jclouds.credential", "It doesn't matter what this value is.")
    
    new BlobStoreContextFactory().createContext("filesystem", properties)
  }
}