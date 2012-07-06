package services.blobs

import play.Play._
import com.google.inject.Inject
import models.{BlobKey, BlobKeyStore}
import services.AppConfig

class DBIndexedS3BlobVendor @Inject() (blobKeyStore: BlobKeyStore, protected val blobVendorDelegate: BlobVendor)
  extends S3BlobVendor(configuration.getProperty("s3.id"), configuration.getProperty("s3.secret"))
  with BlobVendorComposition
{
  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    super.put(namespace, key, data, access)
    val url = super.urlOption(namespace, key).getOrElse("")
    try {
      BlobKey(key = key, url = url).save()
    } catch {
      case e: RuntimeException =>
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

object DBIndexedS3BlobVendor extends DBIndexedS3BlobVendor(AppConfig.instance[BlobKeyStore], S3BlobVendor)
