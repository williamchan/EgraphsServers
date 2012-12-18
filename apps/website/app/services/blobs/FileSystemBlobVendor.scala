package services.blobs

import com.google.inject.Inject
import java.util.Properties
import org.jclouds.filesystem.reference.FilesystemConstants
import org.jclouds.blobstore.BlobStoreContextFactory
import play.api.mvc.Request
import services._
import services.Time.IntsToSeconds._

/**
 * [[services.blobs.BlobVendor]] implementation on the local system.
 */
private[blobs] class FileSystemBlobVendor @Inject() (consumerApp: ConsumerApplication) extends BlobVendor {
  //
  // BlobVendor members
  //
  override def context = {
    val properties = new Properties

    properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, "./tmp/blobstore")

    // jclouds borks if we don't provde any "jclouds.credential" property.
    properties.setProperty("jclouds.credential", "It doesn't matter what this value is.")

    new BlobStoreContextFactory().createContext("filesystem", properties)
  }


  override def urlOption(namespace: String, key: String): Option[String] = {
    val blobStore = context.getBlobStore
    if (!blobStore.blobExists(namespace, key)) {
      None
    }
    else {
      Some(consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getBlob(key).url))
    }
  }

  override def put(namespace: String, key: String, bytes: Array[Byte], access: AccessPolicy) {
    val blobStore = context.getBlobStore

    blobStore.putBlob(
      namespace,
      blobStore.blobBuilder(key).payload(bytes).build()
    )
  }
  
  override def checkConfiguration() {
    // No configuration necessary to use the file system as a blob store
  }
}
