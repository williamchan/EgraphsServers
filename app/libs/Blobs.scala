package libs

import play.Play
import org.jclouds.blobstore.{BlobStoreContextFactory, BlobStoreContext, BlobStore}
import java.util.Properties
import org.jclouds.filesystem.reference.FilesystemConstants
import Play.configuration

object Blobs {
  

  def blobstore: BlobStore = {
    context.getBlobStore
  }

  def context: BlobStoreContext = {
    checkAvailable()

    blobstoreType match {
      case "s3" =>
        s3Context

      case "filesystem" =>
        fileSystemContext
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
  private val blobstoreNamespace = configuration.getProperty("blobstore.container")
  private val s3id = configuration.getProperty("s3.id")
  private val s3secret = configuration.getProperty("s3.secret")

  private def s3Context: BlobStoreContext = {
    new BlobStoreContextFactory().createContext("aws-s3", s3id, s3secret)
  }

  private def fileSystemContext: BlobStoreContext = {
    val properties = new Properties
    properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, "./tmp/blobstore")

    new BlobStoreContextFactory().createContext("filesystem", properties)
  }
}