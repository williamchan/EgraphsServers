package libs

import org.jclouds.blobstore.{BlobStoreContextFactory, BlobStoreContext, BlobStore}
import java.util.Properties
import org.jclouds.filesystem.reference.FilesystemConstants
import play.Play.configuration

object Blobs {
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

  def put(key: String, bytes: Array[Byte]) {
    val blob = blobStore.blobBuilder(key).payload(bytes).build()
    blobStore.putBlob(blobstoreNamespace, blob)
  }

  def delete(key: String) {
    blobStore.removeBlob(blobstoreNamespace, key)
  }

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

  def blobStore: BlobStore = {
    context.getBlobStore
  }

  def init() = {
    checkAvailable()

    if (!blobStore.containerExists(blobstoreNamespace)) {
      blobStore.createContainerInLocation(null, blobstoreNamespace)
    }
  }

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

  private val blobstoreType = configuration.getProperty("blobstore")
  private val blobstoreNamespace = configuration.getProperty("blobstore.namespace")
  private val s3id = configuration.getProperty("s3.id")
  private val s3secret = configuration.getProperty("s3.secret")

  private def s3Context: BlobStoreContext = {
    new BlobStoreContextFactory().createContext("aws-s3", s3id, s3secret)
  }

  private def fileSystemContext: BlobStoreContext = {
    val properties = new Properties

    properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, "./public/blobstore")
    properties.setProperty("jclouds.credential", "herp")
    
    new BlobStoreContextFactory().createContext("filesystem", properties)
  }
}