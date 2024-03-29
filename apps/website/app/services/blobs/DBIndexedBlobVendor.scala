package services.blobs

import com.google.inject.Inject
import models.{BlobKey, BlobKeyStore}
import services.logging.Logging
import services._
import org.jclouds.blobstore.domain.Blob

/**
 * BlobVendor implementation that caches key urls in the database to avoid expensive
 * round-trips on queries that don't require the full Blob data.
 *
 * @param blobKeyStore store for a [[models.BlobKey]]
 * @param blobVendorDelegate vendor to which which this class delegates the actual blob management.
 */
class DBIndexedBlobVendor @Inject()(
  blobKeyStore: BlobKeyStore, 
  protected val blobVendorDelegate: BlobVendor, 
  consumerApp: ConsumerApplication
)
  extends FileSystemBlobVendor(consumerApp)
  with BlobVendorComposition
{

  override def get(namespace: String, key: String): Option[Blob] = {
    blobVendorDelegate.get(namespace, key)
  }

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    blobVendorDelegate.put(namespace, key, data, access)
    val url = blobVendorDelegate.urlOption(namespace, key).getOrElse("")

    blobKeyStore.findByKey(key) match {
      case None => BlobKey(key = key, url = url).save()
      case Some(blobKey) => blobKey.copy(url = url).save()
    }
  }

  override def delete(namespace: String, key: String) {
	throw new UnsupportedOperationException("This feature has not been implemented")
  }

  override def exists(namespace: String, key: String) = {
    blobKeyStore.findByKey(key) match {
      case None => blobVendorDelegate.exists(namespace, key)
      case Some(blobKey) => true
    }
  }

  override def urlOption(namespace: String, key: String) = {
    blobKeyStore.findByKey(key) match {
      case None =>
        val blobVendorUrlOption = blobVendorDelegate.urlOption(namespace, key)

        try {
          blobVendorUrlOption.foreach(url => BlobKey(key=key, url=url).save())
        } catch {
          case e: Exception => Utils.logException(e)
        }

        blobVendorUrlOption
      
      case Some(blobKey) => 
        Option(blobKey.url)
    }
  }
}

object DBIndexedBlobVendor extends Logging