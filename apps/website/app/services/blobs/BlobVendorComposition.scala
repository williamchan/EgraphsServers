package services.blobs

private[blobs] trait BlobVendorComposition extends BlobVendor {

  protected def blobVendorDelegate: BlobVendor

  def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    blobVendorDelegate.put(namespace, key, data, access)
  }

  override def exists(namespace: String, key: String) = {
    blobVendorDelegate.exists(namespace, key)
  }

  override def urlOption(namespace: String, key: String) = {
    blobVendorDelegate.urlOption(namespace, key)
  }
}
