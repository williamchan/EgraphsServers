package services.blobs

import com.google.inject.Inject
import models.{BlobKey, BlobKeyStore}
import services.AppConfig

/**
 * BlobVendor implementation that caches key urls in the database to avoid expensive
 * round-trips on queries that don't require the full Blob data.
 *
 * @param blobKeyStore store for a [[models.BlobKey]]
 * @param blobVendorDelegate vendor to which which this class delegates the actual blob management.
 */
class DBIndexedFileSystemBlobVendor @Inject() (blobKeyStore: BlobKeyStore, protected val blobVendorDelegate: BlobVendor)
  extends FileSystemBlobVendor
  with BlobVendorComposition
{

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    super.put(namespace, key, data, access)
    val url = super.urlOption(namespace, key).getOrElse("")

    blobKeyStore.findByKey(key) match {
      case None => BlobKey(key = key, url = url).save()
      case Some(blobKey) => blobKey.copy(url = url).save()
    }
  }

  override def exists(namespace: String, key: String) = {
    blobKeyStore.findByKey(key) match {
      case None => super.exists(namespace, key)
      case Some(blobKey) => true
    }
  }

  override def urlOption(namespace: String, key: String) = {
    blobKeyStore.findByKey(key) match {
      case None => super.urlOption(namespace, key)
      case Some(blobKey) => Option(blobKey.url)
    }
  }
}

object DBIndexedFileSystemBlobVendor extends DBIndexedFileSystemBlobVendor(AppConfig.instance[BlobKeyStore], FileSystemBlobVendor)
