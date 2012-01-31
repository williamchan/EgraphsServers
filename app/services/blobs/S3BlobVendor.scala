package services.blobs

import play.Play.configuration
import org.jclouds.blobstore.BlobStoreContextFactory
import org.jclouds.aws.s3.AWSS3Client
import org.jclouds.s3.domain.CannedAccessPolicy

/** [[libs.Blobs.BlobProvider]] implementation backed by Amazon S3 */
private[blobs] object S3BlobVendor extends BlobVendor {
  //
  // BlobVendor members
  //
  override val urlBase = "http://s3.amazonaws.com/" + Blobs.blobstoreNamespace
  override def context = {
    new BlobStoreContextFactory().createContext("aws-s3", s3id, s3secret)
  }

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    import org.jclouds.s3.options.PutObjectOptions.Builder.withAcl

    val s3:AWSS3Client = context.getProviderSpecificContext.getApi

    val s3Object = s3.newS3Object()

    s3Object.getMetadata.setKey(key)
    s3Object.setPayload(data)

    s3.putObject(namespace, s3Object, withAcl(s3ConstantForAccessPolicy(access)))
  }

  override def checkConfiguration() {
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

  //
  // Private members
  //
  /** Amazon S3 public id */
  private val s3id = configuration.getProperty("s3.id")

  /** Amazon S3 secret key */
  private val s3secret = configuration.getProperty("s3.secret")

  private def s3ConstantForAccessPolicy(access: AccessPolicy): CannedAccessPolicy = {
    access match {
      case AccessPolicy.Public => CannedAccessPolicy.PUBLIC_READ
      case AccessPolicy.Private => CannedAccessPolicy.PRIVATE
    }
  }
}
